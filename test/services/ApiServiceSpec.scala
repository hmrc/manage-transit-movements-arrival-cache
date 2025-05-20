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

package services

import api.submission.Declaration
import base.{AppWithDefaultMockFixtures, SpecBase}
import connectors.ApiConnector
import generators.Generators
import models.{Arrival, Message, Messages, Version}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

class ApiServiceSpec extends SpecBase with AppWithDefaultMockFixtures with ScalaCheckPropertyChecks with Generators {

  private lazy val mockApiConnector = mock[ApiConnector]
  private lazy val mockDeclaration  = mock[Declaration]

  override def guiceApplicationBuilder(): GuiceApplicationBuilder =
    super
      .guiceApplicationBuilder()
      .overrides(
        bind[ApiConnector].toInstance(mockApiConnector),
        bind[Declaration].toInstance(mockDeclaration)
      )

  private val xml: NodeSeq =
    <ncts:CC007C PhaseID="NCTS5.1" xmlns:ncts="http://ncts.dgtaxud.ec">
      <foo>bar</foo>
    </ncts:CC007C>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApiConnector)
    reset(mockDeclaration)

    when(mockDeclaration.transform(any(), any()))
      .thenReturn(xml)
  }

  private val service = app.injector.instanceOf[ApiService]

  "submitDeclaration" must {
    "call connector" in {
      beforeEach()

      val userAnswers    = emptyUserAnswers
      val expectedResult = HttpResponse(OK, "")

      when(mockApiConnector.submitDeclaration(any())(any())).thenReturn(Future.successful(expectedResult))

      val result = service.submitDeclaration(userAnswers, Version.Phase5).futureValue
      result shouldEqual expectedResult

      verify(mockApiConnector).submitDeclaration(eqTo(xml))(any())

    }
  }

  "get" when {
    val mrn = "27WF9X1FQ9RCKN0TM3"

    "no arrival found" must {
      "return None" in {
        beforeEach()

        when(mockApiConnector.getArrival(any())(any()))
          .thenReturn(Future.successful(None))

        val result = service.get(mrn).futureValue
        result shouldEqual None

        verify(mockApiConnector).getArrival(eqTo(mrn))(any())

      }
    }

    "arrival found" when {
      val arrivalId = "63498209a2d89ad8"

      "messages found" must {
        "return list of messages" in {
          beforeEach()

          when(mockApiConnector.getArrival(any())(any()))
            .thenReturn(Future.successful(Some(Arrival(arrivalId, mrn))))

          val messages = Messages(Seq(Message("IE007", LocalDateTime.now())))

          when(mockApiConnector.getMessages(any())(any()))
            .thenReturn(Future.successful(messages))

          val result = service.get(mrn).futureValue
          result shouldEqual Some(messages)

          verify(mockApiConnector).getArrival(eqTo(mrn))(any())
          verify(mockApiConnector).getMessages(eqTo(arrivalId))(any())

        }
      }
    }
  }
}
