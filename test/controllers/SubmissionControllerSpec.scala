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

import base.{AppWithDefaultMockFixtures, SpecBase}
import models.AuditType.ArrivalNotification
import models.{Message, Messages, Phase, SubmissionStatus}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.{CacheRepository, LockRepository}
import services.{ApiService, AuditService}
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDateTime
import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase with AppWithDefaultMockFixtures {

  private lazy val mockCacheRepository = mock[CacheRepository]
  private lazy val mockLockRepository  = mock[LockRepository]
  private lazy val mockApiService      = mock[ApiService]
  private lazy val mockAuditService    = mock[AuditService]

  override def guiceApplicationBuilder(): GuiceApplicationBuilder =
    super
      .guiceApplicationBuilder()
      .overrides(
        bind[CacheRepository].toInstance(mockCacheRepository),
        bind[LockRepository].toInstance(mockLockRepository),
        bind[ApiService].toInstance(mockApiService),
        bind[AuditService].toInstance(mockAuditService)
      )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCacheRepository)
    reset(mockLockRepository)
    reset(mockApiService)
    reset(mockAuditService)
  }

  "post" should {

    "return 200" when {
      "submission is successful" in {
        val userAnswers = emptyUserAnswers
        when(mockCacheRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswers)))

        val body = Json.toJson("foo")
        when(mockApiService.submitDeclaration(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK, Json.stringify(body))))

        when(mockCacheRepository.set(any()))
          .thenReturn(Future.successful(true))

        when(mockLockRepository.unlock(any(), any()))
          .thenReturn(Future.successful(true))

        val request = FakeRequest(POST, routes.SubmissionController.post().url)
          .withHeaders("API-Version" -> "1.0")
          .withBody(Json.toJson(mrn))

        val result = route(app, request).value

        status(result) shouldEqual OK
        contentAsJson(result) shouldEqual body

        verify(mockCacheRepository).get(eqTo(mrn), eqTo(eoriNumber))
        verify(mockCacheRepository).set(eqTo(userAnswers.metadata.copy(submissionStatus = SubmissionStatus.Submitted)))
        verify(mockLockRepository).unlock(eqTo(eoriNumber), eqTo(mrn))
        verify(mockApiService).submitDeclaration(eqTo(userAnswers), eqTo(Phase.Phase5))(any())
        verify(mockAuditService).audit(eqTo(ArrivalNotification), eqTo(userAnswers))(any())
      }
    }

    "return error" when {
      "submission is invalid" in {
        val userAnswers = emptyUserAnswers
        when(mockCacheRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswers)))

        when(mockApiService.submitDeclaration(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        val request = FakeRequest(POST, routes.SubmissionController.post().url)
          .withHeaders("API-Version" -> "1.0")
          .withBody(Json.toJson(mrn))

        val result = route(app, request).value

        status(result) shouldEqual BAD_REQUEST

        verify(mockCacheRepository).get(eqTo(mrn), eqTo(eoriNumber))
        verify(mockApiService).submitDeclaration(eqTo(userAnswers), eqTo(Phase.Phase5))(any())
      }

      "submission is unsuccessful" in {
        val userAnswers = emptyUserAnswers
        when(mockCacheRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswers)))

        when(mockApiService.submitDeclaration(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

        val request = FakeRequest(POST, routes.SubmissionController.post().url)
          .withHeaders("API-Version" -> "1.0")
          .withBody(Json.toJson(mrn))

        val result = route(app, request).value

        status(result) shouldEqual INTERNAL_SERVER_ERROR

        verify(mockCacheRepository).get(eqTo(mrn), eqTo(eoriNumber))
        verify(mockApiService).submitDeclaration(eqTo(userAnswers), eqTo(Phase.Phase5))(any())
      }

      "document not found in cache" in {
        when(mockCacheRepository.get(any(), any())).thenReturn(Future.successful(None))

        val request = FakeRequest(POST, routes.SubmissionController.post().url)
          .withBody(Json.toJson(mrn))

        val result = route(app, request).value

        status(result) shouldEqual NOT_FOUND

        verify(mockCacheRepository).get(eqTo(mrn), eqTo(eoriNumber))
        verify(mockApiService, never()).submitDeclaration(any(), any())(any())
      }

      "request body can't be validated as a string" in {
        when(mockCacheRepository.get(any(), any())).thenReturn(Future.successful(None))

        val request = FakeRequest(POST, routes.SubmissionController.post().url)
          .withBody(Json.toJson("foo" -> "bar"))

        val result = route(app, request).value

        status(result) shouldEqual BAD_REQUEST

        verify(mockCacheRepository, never()).get(any(), any())
        verify(mockApiService, never()).submitDeclaration(any(), any())(any())
      }
    }
  }

  "get" should {
    val mrn = "27WF9X1FQ9RCKN0TM3"

    "return 200" when {
      "messages found" in {
        val messages = Messages(Seq(Message("IE007", LocalDateTime.now())))

        when(mockApiService.get(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(messages)))

        val request = FakeRequest(GET, routes.SubmissionController.get(mrn).url)
          .withHeaders("API-Version" -> "1.0")

        val result = route(app, request).value

        status(result) shouldEqual OK
        contentAsJson(result) shouldEqual Json.toJson(messages)

        verify(mockApiService).get(eqTo(mrn), eqTo(Phase.Phase5))(any(), any())
      }
    }

    "return 204" when {
      "no messages found" in {
        when(mockApiService.get(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Messages(Nil))))

        val request = FakeRequest(GET, routes.SubmissionController.get(mrn).url)
          .withHeaders("API-Version" -> "1.0")

        val result = route(app, request).value

        status(result) shouldEqual NO_CONTENT

        verify(mockApiService).get(eqTo(mrn), eqTo(Phase.Phase5))(any(), any())
      }
    }

    "return 400" when {
      "invalid API-Version header" in {
        when(mockApiService.get(any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.SubmissionController.get(mrn).url)
          .withHeaders("API-Version" -> "foo")

        val result = route(app, request).value

        status(result) shouldEqual BAD_REQUEST

        verifyNoInteractions(mockApiService)
      }
    }

    "return 404" when {
      "no arrival found" in {
        when(mockApiService.get(any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.SubmissionController.get(mrn).url)
          .withHeaders("API-Version" -> "1.0")

        val result = route(app, request).value

        status(result) shouldEqual NOT_FOUND

        verify(mockApiService).get(eqTo(mrn), eqTo(Phase.Phase5))(any(), any())
      }
    }
  }
}
