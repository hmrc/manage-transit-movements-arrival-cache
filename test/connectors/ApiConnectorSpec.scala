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

import base.{AppWithDefaultMockFixtures, SpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import helper.WireMockServerHandler
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse

import scala.xml.NodeSeq

class ApiConnectorSpec extends SpecBase with AppWithDefaultMockFixtures with WireMockServerHandler {

  override def guiceApplicationBuilder(): GuiceApplicationBuilder =
    super
      .guiceApplicationBuilder()
      .configure(conf = "microservice.services.common-transit-convention-traders.port" -> server.port())

  private lazy val connector: ApiConnector = app.injector.instanceOf[ApiConnector]

  private val arrivalId: String = "someid"

  private val expected: String = Json
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

  "ApiConnector" when {

    "submitDeclaration is called" when {
      val url = "/movements/arrivals"

      val payload: NodeSeq =
        <ncts:CC007C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
          <foo>bar</foo>
        </ncts:CC007C>

      "success" in {
        server.stubFor(post(urlEqualTo(url)).willReturn(okJson(expected)))

        val res = await(connector.submitDeclaration(payload))
        res.toString shouldBe Right(HttpResponse(OK, expected)).toString
      }

      "bad request" in {
        server.stubFor(post(urlEqualTo(url)).willReturn(badRequest()))

        val res = await(connector.submitDeclaration(payload))
        res shouldBe Left(BadRequest("ApiConnector:submitDeclaration: bad request"))
      }

      "internal server error" in {
        server.stubFor(post(urlEqualTo(url)).willReturn(serverError()))

        val res = await(connector.submitDeclaration(payload))
        res shouldBe Left(InternalServerError("ApiConnector:submitDeclaration: something went wrong"))
      }
    }
  }
}
