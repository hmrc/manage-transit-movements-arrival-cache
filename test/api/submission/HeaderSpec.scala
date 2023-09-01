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

import base.SpecBase
import generated.CC007C
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

        val result = Header.message(uA)

        result.messageSender shouldBe uA.eoriNumber
        result.messagE_1Sequence2.messageRecipient shouldBe "NTA.GB"
        result.messagE_1Sequence2.messageIdentification shouldBe "CC007C"
        result.messagE_TYPESequence3.messageType shouldBe CC007C
        result.correlatioN_IDENTIFIERSequence4.correlationIdentifier shouldBe None
      }
    }
  }
}
