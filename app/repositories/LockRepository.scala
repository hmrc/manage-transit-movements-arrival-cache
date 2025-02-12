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

import config.AppConfig
import models.Lock
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Indexes.{ascending, compoundIndex}
import services.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LockRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Lock](
      mongoComponent = mongoComponent,
      collectionName = "draft-locks",
      domainFormat = Lock.format,
      indexes = LockRepository.indexes(appConfig)
    ) {

  private def insertNewLock(lock: Lock): Future[Boolean] =
    collection
      .insertOne(lock)
      .head()
      .map(_.wasAcknowledged())

  private def updateLock(existingLock: Lock): Future[Boolean] = {
    val filters = Filters.and(
      Filters.eq("eoriNumber", existingLock.eoriNumber),
      Filters.eq("mrn", existingLock.mrn)
    )

    val updatedLock = existingLock.copy(lastUpdated = dateTimeService.timestamp)

    collection
      .replaceOne(filters, updatedLock)
      .head()
      .map(_.wasAcknowledged())
  }

  def lock(sessionId: String, eoriNumber: String, mrn: String): Future[Boolean] =
    findLocks(eoriNumber, mrn).flatMap {
      case Some(existingLock) if existingLock.sessionId == sessionId =>
        updateLock(existingLock)
      case None =>
        val now = dateTimeService.timestamp
        val lock = Lock(
          sessionId = sessionId,
          eoriNumber = eoriNumber,
          mrn = mrn,
          createdAt = now,
          lastUpdated = now
        )
        insertNewLock(lock)
      case _ =>
        Future.successful(false)
    }

  private def findLocks(eoriNumber: String, mrn: String): Future[Option[Lock]] = {
    val filters = Filters.and(
      Filters.eq("eoriNumber", eoriNumber),
      Filters.eq("mrn", mrn),
      Filters.gt("lastUpdated", dateTimeService.nMinutesAgo(appConfig.lockTTLInMins))
    )

    collection.find(filters).headOption()
  }

  def unlock(eoriNumber: String, mrn: String, sessionId: String): Future[Boolean] = {
    val filters = Filters.and(
      Filters.eq("sessionId", sessionId),
      Filters.eq("eoriNumber", eoriNumber),
      Filters.eq("mrn", mrn)
    )

    collection
      .deleteOne(filters)
      .head()
      .map(_.wasAcknowledged())
  }

  def unlock(eoriNumber: String, mrn: String): Future[Boolean] = {
    val filters = Filters.and(
      Filters.eq("eoriNumber", eoriNumber),
      Filters.eq("mrn", mrn)
    )

    collection
      .deleteMany(filters)
      .head()
      .map(_.wasAcknowledged())
  }
}

object LockRepository {

  def indexes(appConfig: AppConfig): Seq[IndexModel] = {
    val userAnswersCreatedAtIndex: IndexModel = IndexModel(
      keys = Indexes.ascending("createdAt"),
      indexOptions = IndexOptions().name("draft-lock-created-at-index")
    )

    val userAnswersLastUpdatedIndex: IndexModel = IndexModel(
      keys = Indexes.ascending("lastUpdated"),
      indexOptions = IndexOptions().name("draft-lock-last-updated-index").expireAfter(appConfig.lockTTLInMins.toLong, TimeUnit.MINUTES)
    )

    val eoriNumberAndMrnCompoundIndex: IndexModel = IndexModel(
      keys = compoundIndex(ascending("eoriNumber"), ascending("mrn")),
      indexOptions = IndexOptions().name("eoriNumber-mrn-index")
    )

    Seq(userAnswersCreatedAtIndex, userAnswersLastUpdatedIndex, eoriNumberAndMrnCompoundIndex)
  }
}
