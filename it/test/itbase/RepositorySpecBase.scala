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

package itbase

import models.{Metadata, SubmissionStatus, UserAnswers}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport

import java.time.Instant
import java.util.UUID

trait RepositorySpecBase extends ItSpecBase {
  self: MongoSupport =>

  override def guiceApplicationBuilder(): GuiceApplicationBuilder =
    super
      .guiceApplicationBuilder()
      .overrides(bind[MongoComponent].toInstance(mongoComponent))

  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val baseUrl            = s"http://localhost:$port"

  val mrn        = "mrn"
  val eoriNumber = "eori"

  def emptyMetadata: Metadata = Metadata(
    mrn = mrn,
    eoriNumber = eoriNumber,
    data = Json.obj(),
    submissionStatus = SubmissionStatus.NotSubmitted
  )

  def emptyUserAnswers: UserAnswers = UserAnswers(
    metadata = emptyMetadata,
    createdAt = Instant.now(),
    lastUpdated = Instant.now(),
    id = UUID.randomUUID()
  )
}
