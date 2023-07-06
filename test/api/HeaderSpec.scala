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

package api

import api.submission.Header
import base.SpecBase
import generated.{CORRELATION_IDENTIFIERSequence, MESSAGE_1Sequence, MESSAGE_FROM_TRADERSequence}
import models.UserAnswers
import play.api.libs.json.{JsValue, Json}

class HeaderSpec extends SpecBase {

  "Conversions" when {

    "message is called" should {

      val json: JsValue = Json.parse(s"""
          |{
          |  "_id" : "$uuid",
          |  "mrn" : "$mrn",
          |  "eoriNumber" : "$eoriNumber",
          |  "isSubmitted" : false,
          |  "data" : {
          |    "identification" : {
          |      "destinationOffice" : {
          |        "id" : "GB000142",
          |        "name" : "Belfast EPU",
          |        "phoneNumber" : "+44 (0)3000 523068"
          |      }
          |    }
          |  },
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

      "convert to API format" in {

        val converted = Header.message(uA)

        val expected = MESSAGE_FROM_TRADERSequence(
          messageSender = Some("NCTS"),
          messagE_1Sequence2 = MESSAGE_1Sequence(
            messageRecipient = "NTA.GB",
            preparationDateAndTime = converted.messagE_1Sequence2.preparationDateAndTime,
            messageIdentification = "CC007C"
          )
        )

        converted shouldBe expected

      }

    }

    "messageType is called" should {

      "convert to API format" in {

        Header.messageType.toString shouldBe "CC007C"

      }

    }

    "correlationIdentifier is called" should {

      "convert to API format" in {

        Header.correlationIdentifier shouldBe CORRELATION_IDENTIFIERSequence(None)

      }

    }

  }
}
