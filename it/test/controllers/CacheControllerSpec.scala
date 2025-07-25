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

package controllers

import itbase.CacheRepositorySpecBase
import models.{Metadata, SubmissionStatus, UserAnswers}
import org.mongodb.scala.model.Filters
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.libs.ws.JsonBodyWritables._

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID

class CacheControllerSpec extends CacheRepositorySpecBase {

  "GET /user-answers/:mrn" when {

    val url = s"$baseUrl/manage-transit-movements-arrival-cache/user-answers/$mrn"

    "document does not exist" should {
      "respond with 404 status" in {
        val response = wsClient
          .url(url)
          .get()
          .futureValue

        response.status shouldEqual 404
      }
    }

    "document does exist" when {
      "APIVersion header aligns with document" must {
        "respond with 200 status" in {
          val userAnswers = emptyUserAnswers
          insert(userAnswers).futureValue

          val response = wsClient
            .url(url)
            .get()
            .futureValue

          response.status shouldEqual 200

          response.json.as[UserAnswers].metadata shouldEqual userAnswers.metadata

          response.json.as[UserAnswers].createdAt shouldEqual userAnswers.createdAt.truncatedTo(
            java.time.temporal.ChronoUnit.MILLIS
          )

          response.json.as[UserAnswers].lastUpdated shouldEqual userAnswers.lastUpdated.truncatedTo(
            java.time.temporal.ChronoUnit.MILLIS
          )
        }
      }
    }
  }

  "POST /user-answers" when {

    val url = s"$baseUrl/manage-transit-movements-arrival-cache/user-answers/$mrn"

    "document successfully written to mongo" should {
      "respond with 200 status" in {
        val metadata = emptyMetadata

        val response = wsClient
          .url(url)
          .post(Json.toJson(metadata))
          .futureValue

        response.status shouldEqual 200

        val results = findAll().futureValue
        results.size shouldEqual 1
        val result = results.head
        result.mrn shouldEqual metadata.mrn
        result.eoriNumber shouldEqual metadata.eoriNumber
        result.metadata shouldEqual metadata
      }
    }

    "empty request body" should {
      "respond with 400 status" in {
        val response = wsClient
          .url(url)
          .post(Json.obj())
          .futureValue

        response.status shouldEqual 400
      }
    }

    "invalid request body" should {
      "respond with 400 status" in {
        val response = wsClient
          .url(url)
          .post(JsString("foo"))
          .futureValue

        response.status shouldEqual 400
      }
    }

    "the EORI in the enrolment and the EORI in user answers do not match" should {
      "respond with 403 status" in {
        val metadata    = emptyMetadata.copy(eoriNumber = "different eori")
        val userAnswers = emptyUserAnswers.copy(metadata = metadata)

        val response = wsClient
          .url(url)
          .post(Json.toJson(userAnswers))
          .futureValue

        response.status shouldEqual 403
      }
    }
  }

  "PUT /user-answers" when {

    val url = s"$baseUrl/manage-transit-movements-arrival-cache/user-answers"

    "document successfully written to mongo" should {
      "respond with 200 status" in {
        val response = wsClient
          .url(url)
          .put(JsString(mrn))
          .futureValue

        response.status shouldEqual 200

        val filters = Filters.and(
          Filters.eq("mrn", mrn),
          Filters.eq("eoriNumber", eoriNumber)
        )
        val results = find(filters).futureValue
        results.size shouldEqual 1
      }
    }

    "empty request body" should {
      "respond with 400 status" in {
        val response = wsClient
          .url(url)
          .put(Json.obj())
          .futureValue

        response.status shouldEqual 400
      }
    }

    "invalid request body" should {
      "respond with 400 status" in {
        val response = wsClient
          .url(url)
          .put(Json.obj("foo" -> "bar"))
          .futureValue

        response.status shouldEqual 400
      }
    }
  }

  "DELETE /user-answers/:mrn" when {

    val url = s"$baseUrl/manage-transit-movements-arrival-cache/user-answers/$mrn"

    "document exists" should {
      "remove document and respond with 200 status" in {
        insert(emptyUserAnswers).futureValue

        val response = wsClient
          .url(url)
          .delete()
          .futureValue

        response.status shouldEqual 200

        findAll().futureValue shouldBe empty
      }
    }

    "document does not exist" should {
      "respond with 200 status" in {
        val response = wsClient
          .url(url)
          .delete()
          .futureValue

        response.status shouldEqual 200
      }
    }
  }

  "GET /user-answers" when {

    val url = s"$baseUrl/manage-transit-movements-arrival-cache/user-answers"

    "documents do exist" should {
      "respond with 200 status" in {
        val userAnswers1 = UserAnswers(
          metadata = Metadata("AB123", eoriNumber, Json.obj(), SubmissionStatus.NotSubmitted),
          createdAt = Instant.now(),
          lastUpdated = Instant.now(),
          id = UUID.randomUUID()
        )

        val userAnswers2 = UserAnswers(
          metadata = Metadata("CD123", eoriNumber, Json.obj(), SubmissionStatus.NotSubmitted),
          createdAt = Instant.now().minus(1, DAYS),
          lastUpdated = Instant.now().minus(1, DAYS),
          id = UUID.randomUUID()
        )

        insert(userAnswers1).futureValue
        insert(userAnswers2).futureValue

        val response = wsClient
          .url(url)
          .get()
          .futureValue

        response.status shouldEqual 200

        (response.json \ "userAnswers").as[Seq[JsObject]].length shouldEqual 2

        val mrnResults = response.json \ "userAnswers" \\ "mrn"

        mrnResults.head.validate[String].get shouldEqual "AB123"
        mrnResults(1).validate[String].get shouldEqual "CD123"

        val urlResults = response.json \ "userAnswers" \\ "_links"

        (urlResults.head \ "self" \ "href").validate[String].get shouldEqual controllers.routes.CacheController.get("AB123").url
        (urlResults(1) \ "self" \ "href").validate[String].get shouldEqual controllers.routes.CacheController.get("CD123").url
      }
    }
  }
}
