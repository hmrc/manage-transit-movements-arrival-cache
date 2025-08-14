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
import base.SpecBase
import connectors.ApiConnector
import generators.Generators
import models.{Arrival, Message, Messages, Phase}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status.OK
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

class ApiServiceSpec extends SpecBase with BeforeAndAfterEach with ScalaCheckPropertyChecks with Generators {

  private lazy val mockApiConnector = mock[ApiConnector]
  private lazy val mockDeclaration  = mock[Declaration]

  private val xml: NodeSeq =
    <ncts:CC007C PhaseID="NCTS5.1" xmlns:ncts="http://ncts.dgtaxud.ec">
      <foo>bar</foo>
    </ncts:CC007C>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApiConnector)
    reset(mockDeclaration)
  }

  private val service = new ApiService(mockApiConnector, mockDeclaration)

  "submitDeclaration" must {
    "call connector" in {
      forAll(arbitrary[Phase]) {
        version =>
          beforeEach()

          val userAnswers    = emptyUserAnswers
          val expectedResult = HttpResponse(OK, "")

          when(mockDeclaration.transform(any(), any()))
            .thenReturn(xml)

          when(mockApiConnector.submitDeclaration(any(), any())(any())).thenReturn(Future.successful(expectedResult))

          val result = service.submitDeclaration(userAnswers, version).futureValue
          result shouldEqual expectedResult

          verify(mockApiConnector).submitDeclaration(eqTo(xml), eqTo(version))(any())
      }
    }
  }

  "get" when {
    val mrn = "27WF9X1FQ9RCKN0TM3"

    "no arrival found" must {
      "return None" in {
        forAll(arbitrary[Phase]) {
          version =>
            beforeEach()

            when(mockApiConnector.getArrival(any(), any())(any()))
              .thenReturn(Future.successful(None))

            val result = service.get(mrn, version).futureValue
            result shouldEqual None

            verify(mockApiConnector).getArrival(eqTo(mrn), eqTo(version))(any())
        }
      }
    }

    "arrival found" when {
      val arrivalId = "63498209a2d89ad8"

      "messages found" must {
        "return list of messages" in {
          forAll(arbitrary[Phase]) {
            version =>
              beforeEach()

              when(mockApiConnector.getArrival(any(), any())(any()))
                .thenReturn(Future.successful(Some(Arrival(arrivalId, mrn))))

              val messages = Messages(Seq(Message("IE007", LocalDateTime.now())))

              when(mockApiConnector.getMessages(any(), any())(any()))
                .thenReturn(Future.successful(messages))

              val result = service.get(mrn, version).futureValue
              result shouldEqual Some(messages)

              verify(mockApiConnector).getArrival(eqTo(mrn), eqTo(version))(any())
              verify(mockApiConnector).getMessages(eqTo(arrivalId), eqTo(version))(any())
          }
        }
      }
    }
  }
}
