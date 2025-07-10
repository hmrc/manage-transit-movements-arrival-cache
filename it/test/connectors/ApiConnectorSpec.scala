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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import itbase.{ItSpecBase, WireMockServerHandler}
import models.Phase.*
import models.{Arrival, Message, Messages}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.xml.NodeSeq

class ApiConnectorSpec extends ItSpecBase with WireMockServerHandler {

  implicit override val hc: HeaderCarrier = HeaderCarrier(
    otherHeaders = Seq(ACCEPT -> "application/vnd.hmrc.2.1+json")
  )

  override def guiceApplicationBuilder(): GuiceApplicationBuilder =
    super
      .guiceApplicationBuilder()
      .configure(conf = "microservice.services.common-transit-convention-traders.port" -> server.port())

  private lazy val connector: ApiConnector = app.injector.instanceOf[ApiConnector]

  private val arrivalId: String = "63498209a2d89ad8"

  "ApiConnector" when {

    "submitDeclaration is called" when {
      val url = "/movements/arrivals"

      val payload: NodeSeq =
        <ncts:CC007C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
          <foo>bar</foo>
        </ncts:CC007C>

      val response: String = Json
        .obj(
          "_links" -> Json.obj(
            "self" -> Json.obj(
              "href" -> s"/customs/transits/movements/arrivals/$arrivalId"
            ),
            "messages" -> Json.obj(
              "href" -> s"/customs/transits/movements/arrivals/$arrivalId/messages"
            )
          )
        )
        .toString()
        .stripMargin

      "success" in {
        server.stubFor(
          post(urlEqualTo(url))
            .withHeader(ACCEPT, equalTo("application/vnd.hmrc.2.1+json"))
            .willReturn(okJson(response))
        )

        val res = await(connector.submitDeclaration(payload, Phase5))
        res.status shouldEqual OK
      }

      "bad request" in {
        server.stubFor(
          post(urlEqualTo(url))
            .withHeader(ACCEPT, equalTo("application/vnd.hmrc.2.1+json"))
            .willReturn(badRequest())
        )

        val res = await(connector.submitDeclaration(payload, Phase5))
        res.status shouldEqual BAD_REQUEST
      }

      "internal server error" in {
        server.stubFor(
          post(urlEqualTo(url))
            .withHeader(ACCEPT, equalTo("application/vnd.hmrc.2.1+json"))
            .willReturn(serverError())
        )

        val res = await(connector.submitDeclaration(payload, Phase5))
        res.status shouldEqual INTERNAL_SERVER_ERROR
      }
    }

    "getArrival" when {
      val mrn = "27WF9X1FQ9RCKN0TM3"
      val url = s"/movements/arrivals?movementReferenceNumber=$mrn"

      "success" in {
        val response: String =
          s"""
             |{
             |  "_links": {
             |    "self": {
             |      "href": "/customs/transits/movements/arrivals"
             |    }
             |  },
             |  "totalCount": 1,
             |  "arrivals": [
             |    {
             |      "_links": {
             |        "self": {
             |          "href": "/customs/transits/movements/arrivals/$arrivalId"
             |        },
             |        "messages": {
             |          "href": "/customs/transits/movements/arrivals/$arrivalId/messages"
             |        }
             |      },
             |      "id": "63651574c3447b12",
             |      "movementReferenceNumber": "$mrn",
             |      "created": "2022-11-04T13:36:52.332Z",
             |      "updated": "2022-11-04T13:36:52.332Z",
             |      "enrollmentEORINumber": "9999912345",
             |      "movementEORINumber": "GB1234567890"
             |    }
             |  ]
             |}
             |""".stripMargin

        server.stubFor(
          get(urlEqualTo(url))
            .withHeader(ACCEPT, equalTo("application/vnd.hmrc.2.1+json"))
            .willReturn(okJson(response))
        )

        val res = await(connector.getArrival(mrn, Phase5))
        res shouldEqual Some(
          Arrival(
            id = "63651574c3447b12",
            movementReferenceNumber = mrn
          )
        )
      }

      "no messages found" in {
        val response: String =
          s"""
             |{
             |  "_links": {
             |    "self": {
             |      "href": "/customs/transits/movements/arrivals"
             |    }
             |  },
             |  "totalCount": 0,
             |  "arrivals": []
             |}
             |""".stripMargin

        server.stubFor(
          get(urlEqualTo(url))
            .withHeader(ACCEPT, equalTo("application/vnd.hmrc.2.1+json"))
            .willReturn(okJson(response))
        )

        val res = await(connector.getArrival(mrn, Phase5))
        res shouldEqual None
      }
    }

    "getMessages" when {
      val url = s"/movements/arrivals/$arrivalId/messages"

      val messageId = "634982098f02f00a"

      val response: String =
        s"""
          |{
          |  "_links": {
          |    "self": {
          |      "href": "/customs/transits/movements/arrivals/$arrivalId/messages"
          |    },
          |    "arrival": {
          |      "href": "/customs/transits/movements/arrivals/$arrivalId"
          |    }
          |  },
          |  "totalCount": 1,
          |  "messages": [
          |    {
          |      "_links": {
          |        "self": {
          |          "href": "/customs/transits/movements/arrivals/$arrivalId/messages/634982098f02f00a"
          |        },
          |        "arrival": {
          |          "href": "/customs/transits/movements/arrivals/$arrivalId"
          |        }
          |      },
          |      "id": "$messageId",
          |      "arrivalId": "$arrivalId",
          |      "received": "2022-11-10T15:32:51.459Z",
          |      "type": "IE007",
          |      "status": "Success"
          |    }
          |  ]
          |}
          |""".stripMargin

      "success" in {
        server.stubFor(
          get(urlEqualTo(url))
            .withHeader(ACCEPT, equalTo("application/vnd.hmrc.2.1+json"))
            .willReturn(okJson(response))
        )

        val res = await(connector.getMessages(arrivalId, Phase5))
        res shouldEqual Messages(
          Seq(
            Message("IE007", LocalDateTime.of(2022, 11, 10, 15, 32, 51, 459000000))
          )
        )
      }
    }
  }
}
