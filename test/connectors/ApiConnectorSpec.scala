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

import base.AppWithDefaultMockFixtures
import com.github.tomakehurst.wiremock.client.WireMock._
import helper.WireMockServerHandler
import models.UserAnswers
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{BadRequest, InternalServerError}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

class ApiConnectorSpec extends AnyFreeSpec with AppWithDefaultMockFixtures with WireMockServerHandler with Matchers {

  val mrn        = "mrn"
  val eoriNumber = "eori"
  val uuid       = "2e8ede47-dbfb-44ea-a1e3-6c57b1fe6fe2"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val json: JsValue = Json.parse(s"""
                                    |{
                                    |  "_id" : "$uuid",
                                    |  "mrn" : "$mrn",
                                    |  "eoriNumber" : "$eoriNumber",
                                    |  "data" : {
                                    |    "identification" : {
                                    |      "destinationOffice" : {
                                    |        "id" : "GB000142",
                                    |        "name" : "Belfast EPU",
                                    |        "phoneNumber" : "+44 (0)3000 523068"
                                    |      },
                                    |      "identificationNumber" : "GB123456789000",
                                    |      "isSimplifiedProcedure" : "normal"
                                    |    },
                                    |    "locationOfGoods" : {
                                    |      "typeOfLocation" : {
                                    |        "type": "B",
                                    |        "description": "Authorised place"
                                    |      },
                                    |      "qualifierOfIdentification" : {
                                    |        "qualifier": "V",
                                    |        "description": "Customs office identifier"
                                    |      },
                                    |      "qualifierOfIdentificationDetails" : {
                                    |        "customsOffice" : {
                                    |          "id" : "GB000142",
                                    |          "name" : "Belfast EPU",
                                    |          "phoneNumber" : "+44 (0)3000 523068"
                                    |        }
                                    |      }
                                    |    },
                                    |    "incidentFlag" : true,
                                    |    "incidents" : [
                                    |      {
                                    |        "incidentCountry" : {
                                    |          "code" : "FR",
                                    |          "description" : "France"
                                    |        },
                                    |        "incidentCode" : {
                                    |          "code": "4",
                                    |          "description": "Imminent danger necessitates immediate partial or total unloading of the sealed means of transport."
                                    |        },
                                    |        "incidentText" : "foo",
                                    |        "addEndorsement" : true,
                                    |        "endorsement" : {
                                    |          "date" : "2022-01-01",
                                    |          "authority" : "bar",
                                    |          "country" : {
                                    |            "code" : "FR",
                                    |            "description" : "France"
                                    |          },
                                    |          "location" : "foobar"
                                    |        },
                                    |        "qualifierOfIdentification" : {
                                    |          "qualifier": "U",
                                    |          "description": "UN/LOCODE"
                                    |        },
                                    |        "unLocode" : "DEAAL",
                                    |        "equipments" : [
                                    |          {
                                    |            "containerIdentificationNumberYesNo" : true,
                                    |            "containerIdentificationNumber" : "1",
                                    |            "addSealsYesNo" : true,
                                    |            "seals" : [
                                    |              {
                                    |                "sealIdentificationNumber" : "1"
                                    |              }
                                    |            ],
                                    |            "addGoodsItemNumberYesNo" : true,
                                    |            "itemNumbers" : [
                                    |              {
                                    |                "itemNumber" : "1"
                                    |              }
                                    |            ]
                                    |          }
                                    |        ]
                                    |      }
                                    |    ]
                                    |  },
                                    |  "tasks" : {},
                                    |  "createdAt" : {
                                    |    "$$date" : {
                                    |      "$$numberLong" : "1662393524188"
                                    |    }
                                    |  },
                                    |  "lastUpdated" : {
                                    |    "$$date" : {
                                    |      "$$numberLong" : "1662546803472"
                                    |    }
                                    |  }
                                    |}
                                    |""".stripMargin)

  val uA: UserAnswers = json.as[UserAnswers](UserAnswers.mongoFormat)

  override def guiceApplicationBuilder(): GuiceApplicationBuilder =
    super
      .guiceApplicationBuilder()
      .configure(conf = "microservice.services.common-transit-convention-traders.port" -> server.port())

  private lazy val connector: ApiConnector = app.injector.instanceOf[ApiConnector]

  val arrivalId: String = "someid"

  val expected: String = Json
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

  val uri = "/movements/arrivals"

  "ApiConnector" - {

    "submitDeclaration is called" - {

      "for success" in {

        server.stubFor(post(urlEqualTo(uri)).willReturn(okJson(expected)))

        val res = await(connector.submitDeclaration(uA))
        res.toString mustBe Right(HttpResponse(OK, expected)).toString

      }

      "for bad request" in {

        server.stubFor(post(urlEqualTo(uri)).willReturn(badRequest()))

        val res = await(connector.submitDeclaration(uA))
        res mustBe Left(BadRequest("ApiConnector:submitDeclaration: bad request"))

      }

      "for internal server error" in {

        server.stubFor(post(urlEqualTo(uri)).willReturn(serverError()))

        val res = await(connector.submitDeclaration(uA))
        res mustBe Left(InternalServerError("ApiConnector:submitDeclaration: something went wrong"))

      }

    }

  }

}
