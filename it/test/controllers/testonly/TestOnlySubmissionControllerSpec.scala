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

package controllers.testonly

import itbase.CacheRepositorySpecBase
import models.{SensitiveFormats, UserAnswers}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.WSClient
import play.api.test.Helpers.running

class TestOnlySubmissionControllerSpec extends CacheRepositorySpecBase {

  override def guiceApplicationBuilder(): GuiceApplicationBuilder =
    super
      .guiceApplicationBuilder()
      .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")

  "POST /test-only/declaration/submit" when {

    val url = s"$baseUrl/manage-transit-movements-arrival-cache/test-only/declaration/submit"

    "body cannot be read as UserAnswers" should {
      "respond with 400 status" in {
        val json = Json.parse("""
            |{
            |  "foo" : "bar"
            |}
            |""".stripMargin)
        val response = wsClient
          .url(url)
          .post(json)
          .futureValue

        response.status shouldEqual 400
      }
    }

    "body cannot be transformed to XML" should {
      "respond with 500 status" in {
        val response = wsClient
          .url(url)
          .post(Json.toJson(emptyUserAnswers))
          .futureValue

        response.status shouldEqual 500
      }
    }

    "body is valid" when {

      val data = Json
        .parse("""
            |{
            |  "identification" : {
            |    "destinationOffice" : {
            |      "id" : "GB000011",
            |      "name" : "Birmingham Airport",
            |      "countryId" : "GB",
            |      "phoneNumber" : "+44 (0)3000 739684"
            |    },
            |    "isSimplifiedProcedure" : "normal",
            |    "identificationNumber" : "GB123456789000"
            |  },
            |  "locationOfGoods" : {
            |    "typeOfLocation" : {
            |      "type" : "A",
            |      "description" : "Designated location"
            |    },
            |    "qualifierOfIdentification" : {
            |      "qualifier" : "U",
            |      "description" : "UN/LOCODE"
            |    },
            |    "qualifierOfIdentificationDetails" : {
            |      "unlocode" : "DEBER"
            |    },
            |    "addContactPerson" : false
            |  }
            |}
            |""".stripMargin)
        .as[JsObject]

      "body is not encrypted" should {
        "respond with 200 status" in {
          val app = guiceApplicationBuilder()
            .configure("encryption.enabled" -> false)
            .build()

          running(app) {
            val metadata    = emptyMetadata.copy(data = data)
            val userAnswers = emptyUserAnswers.copy(metadata = metadata)

            val wsClient: WSClient = app.injector.instanceOf[WSClient]

            implicit val sensitiveFormats: SensitiveFormats = app.injector.instanceOf[SensitiveFormats]

            val response = wsClient
              .url(url)
              .post(Json.toJson(userAnswers)(UserAnswers.sensitiveFormat))
              .futureValue

            response.status shouldEqual 200
          }
        }
      }

      "body is encrypted" should {
        "respond with 200 status" in {
          val app = guiceApplicationBuilder()
            .configure("encryption.enabled" -> true)
            .build()

          running(app) {
            val metadata    = emptyMetadata.copy(data = data)
            val userAnswers = emptyUserAnswers.copy(metadata = metadata)

            val wsClient: WSClient = app.injector.instanceOf[WSClient]

            implicit val sensitiveFormats: SensitiveFormats = app.injector.instanceOf[SensitiveFormats]

            val response = wsClient
              .url(url)
              .post(Json.toJson(userAnswers)(UserAnswers.sensitiveFormat))
              .futureValue

            response.status shouldEqual 200
          }
        }
      }
    }
  }
}
