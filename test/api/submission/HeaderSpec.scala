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

package api.submission

import base.{AppWithDefaultMockFixtures, SpecBase}
import generated.CC007C
import models.UserAnswers
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import scalaxb.XMLCalendar
import services.{DateTimeService, MessageIdentificationService}

import java.time.LocalDateTime

class HeaderSpec extends SpecBase with AppWithDefaultMockFixtures with ScalaCheckPropertyChecks {

  private lazy val mockDateTimeService              = mock[DateTimeService]
  private lazy val mockMessageIdentificationService = mock[MessageIdentificationService]

  override def guiceApplicationBuilder(): GuiceApplicationBuilder =
    super
      .guiceApplicationBuilder()
      .overrides(
        bind[DateTimeService].toInstance(mockDateTimeService),
        bind[MessageIdentificationService].toInstance(mockMessageIdentificationService)
      )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockDateTimeService)
    reset(mockMessageIdentificationService)
  }

  private val header: Header = app.injector.instanceOf[Header]

  private val dateTime: LocalDateTime =
    LocalDateTime.of(2020, 1, 1, 0, 0, 0)

  "Header" when {

    "message is called" should {

      def uA(officeOfDestinationCountryId: String): UserAnswers = {
        val json: JsValue = Json.parse(s"""
             |{
             |  "_id" : "$uuid",
             |  "mrn" : "$mrn",
             |  "eoriNumber" : "$eoriNumber",
             |  "isSubmitted" : false,
             |  "data" : {
             |    "identification" : {
             |      "destinationOffice" : {
             |        "id" : "foo",
             |        "name" : "Belfast EPU",
             |        "phoneNumber" : "+44 (0)3000 523068",
             |        "countryId" : "$officeOfDestinationCountryId"
             |      }
             |    }
             |  },
             |  "createdAt" : "2022-09-05T15:58:44.188Z",
             |  "lastUpdated" : "2022-09-07T10:33:23.472Z",
             |  "submissionStatus" : "notSubmitted"
             |}
             |""".stripMargin)

        json.as[UserAnswers]
      }

      "convert to API format" when {
        "GB office of departure" in {
          forAll(Gen.alphaNumStr) {
            messageIdentification =>
              when(mockDateTimeService.now).thenReturn(dateTime)
              when(mockMessageIdentificationService.randomIdentifier).thenReturn(messageIdentification)

              val result = header.message(uA("GB"))

              result.messageSender shouldEqual eoriNumber
              result.messageRecipient shouldEqual "NTA.GB"
              result.preparationDateAndTime shouldEqual XMLCalendar("2020-01-01T00:00:00")
              result.messageIdentification shouldEqual messageIdentification
              result.messageType shouldEqual CC007C
              result.correlationIdentifier should not be defined
          }
        }

        "XI office of departure" in {
          forAll(Gen.alphaNumStr) {
            messageIdentification =>
              when(mockDateTimeService.now).thenReturn(dateTime)
              when(mockMessageIdentificationService.randomIdentifier).thenReturn(messageIdentification)

              val result = header.message(uA("XI"))

              result.messageSender shouldEqual eoriNumber
              result.messageRecipient shouldEqual "NTA.XI"
              result.preparationDateAndTime shouldEqual XMLCalendar("2020-01-01T00:00:00")
              result.messageIdentification shouldEqual messageIdentification
              result.messageType shouldEqual CC007C
              result.correlationIdentifier should not be defined
          }
        }
      }

      "fail to convert to API format" when {
        "undefined office of departure" in {
          forAll(Gen.alphaNumStr) {
            messageIdentification =>
              when(mockDateTimeService.now).thenReturn(dateTime)
              when(mockMessageIdentificationService.randomIdentifier).thenReturn(messageIdentification)

              val json: JsValue = Json.parse(s"""
                   |{
                   |  "_id" : "$uuid",
                   |  "mrn" : "$mrn",
                   |  "eoriNumber" : "$eoriNumber",
                   |  "isSubmitted" : false,
                   |  "data" : {},
                   |  "createdAt" : "2022-09-05T15:58:44.188Z",
                   |  "lastUpdated" : "2022-09-07T10:33:23.472Z",
                   |  "submissionStatus" : "notSubmitted"
                   |}
                   |""".stripMargin)

              an[Exception] should be thrownBy header.message(json.as[UserAnswers])
          }
        }
      }
    }
  }
}
