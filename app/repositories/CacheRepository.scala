/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repositories

import com.mongodb.client.model.Filters.{regex, and => mAnd, eq => mEq}
import config.AppConfig
import models._
import org.bson.conversions.Bson
import org.mongodb.scala.model.Indexes.{ascending, compoundIndex}
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.{Clock, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CacheRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  clock: Clock
)(implicit ec: ExecutionContext, sensitiveFormats: SensitiveFormats)
    extends PlayMongoRepository[UserAnswers](
      mongoComponent = mongoComponent,
      collectionName = CacheRepository.collectionName,
      domainFormat = UserAnswers.sensitiveFormat,
      indexes = CacheRepository.indexes(appConfig)
    ) {

  def get(mrn: String, eoriNumber: String): Future[Option[UserAnswers]] = {
    val filter = Filters.and(
      Filters.eq("mrn", mrn),
      Filters.eq("eoriNumber", eoriNumber)
    )
    val update  = Updates.set("lastUpdated", Instant.now(clock))
    val options = FindOneAndUpdateOptions().upsert(false).sort(Sorts.descending("createdAt"))

    collection
      .findOneAndUpdate(filter, update, options)
      .toFutureOption()
  }

  def set(data: Metadata): Future[Boolean] = {
    val now = Instant.now(clock)
    val filter = Filters.and(
      Filters.eq("mrn", data.mrn),
      Filters.eq("eoriNumber", data.eoriNumber)
    )
    val updates = Updates.combine(
      Updates.setOnInsert("mrn", data.mrn),
      Updates.setOnInsert("eoriNumber", data.eoriNumber),
      Updates.set("data", Codecs.toBson(data.data)(sensitiveFormats.jsObjectWrites)),
      Updates.setOnInsert("createdAt", now),
      Updates.set("lastUpdated", now),
      Updates.setOnInsert("_id", Codecs.toBson(UUID.randomUUID()))
    )
    val options = UpdateOptions().upsert(true)

    collection
      .updateOne(filter, updates, options)
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def remove(mrn: String, eoriNumber: String): Future[Boolean] = {
    val filter = Filters.and(
      Filters.eq("mrn", mrn),
      Filters.eq("eoriNumber", eoriNumber)
    )

    collection
      .deleteOne(filter)
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def getAll(
    eoriNumber: String,
    mrn: Option[String] = None,
    limit: Option[Int] = None,
    skip: Option[Int] = None,
    sortBy: Option[String] = None
  ): Future[UserAnswersSummary] = {

    val skipIndex: Int   = skip.getOrElse(0)
    val returnLimit: Int = limit.getOrElse(appConfig.maxRowsReturned)
    val skipLimit: Int   = skipIndex * returnLimit
    val mrnRegex         = mrn.map(_.replace(" ", "")).getOrElse("")

    val eoriFilter: Bson = mEq("eoriNumber", eoriNumber)
    val mrnFilter: Bson  = regex("mrn", mrnRegex)

    val primaryFilter = Aggregates.filter(mAnd(eoriFilter, mrnFilter))

    val aggregates: Seq[Bson] = Seq(
      primaryFilter,
      Aggregates.sort(Sort(sortBy).toBson),
      Aggregates.skip(skipLimit),
      Aggregates.limit(returnLimit)
    )

    for {
      totalDocuments         <- collection.countDocuments(eoriFilter).toFuture()
      totalMatchingDocuments <- collection.aggregate[UserAnswers](Seq(primaryFilter)).toFuture().map(_.length)
      aggregateResult        <- collection.aggregate[UserAnswers](aggregates).toFuture()
    } yield UserAnswersSummary(
      eoriNumber,
      aggregateResult,
      appConfig.mongoTtlInDays,
      totalDocuments.toInt,
      totalMatchingDocuments
    )
  }
}

object CacheRepository {

  val collectionName: String = "user-answers"

  def indexes(appConfig: AppConfig): Seq[IndexModel] = {
    val userAnswersCreatedAtIndex: IndexModel = IndexModel(
      keys = Indexes.ascending("createdAt"),
      indexOptions = IndexOptions().name("user-answers-created-at-index").expireAfter(appConfig.mongoTtlInDays, TimeUnit.DAYS)
    )

    val eoriNumberAndMrnCompoundIndex: IndexModel = IndexModel(
      keys = compoundIndex(ascending("eoriNumber"), ascending("mrn")),
      indexOptions = IndexOptions().name("eoriNumber-mrn-index")
    )

    Seq(userAnswersCreatedAtIndex, eoriNumberAndMrnCompoundIndex)
  }

}
