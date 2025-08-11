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
import generated.*
import models.{Phase, UserAnswers}
import play.api.libs.json.{JsValue, Json}

class TransitOperationSpec extends SpecBase {

  "TransitOperation" when {

    "transform is called" should {

      "convert to API format" when {

        "phase 5" when {

          "normal procedure" in {

            val json: JsValue = Json.parse(s"""
                 |{
                 |  "_id" : "c8fdf8a7-1c77-4d25-991d-2a0881e05062",
                 |  "mrn" : "$mrn",
                 |  "eoriNumber" : "GB1234567",
                 |  "data" : {
                 |    "identification" : {
                 |      "destinationOffice" : {
                 |        "id" : "GB000051",
                 |        "name" : "Felixstowe",
                 |        "phoneNumber" : "+44 (0)1394 303023 / 24 / 26"
                 |      },
                 |      "identificationNumber" : "GB123456789000",
                 |      "isSimplifiedProcedure" : "normal"
                 |    },
                 |    "locationOfGoods" : {
                 |      "typeOfLocation" : "authorisedPlace",
                 |      "qualifierOfIdentification" : "customsOffice",
                 |      "qualifierOfIdentificationDetails" : {
                 |        "customsOffice" : {
                 |          "id" : "GB000142",
                 |          "name" : "Belfast EPU",
                 |          "phoneNumber" : "+44 (0)3000 523068"
                 |        }
                 |      }
                 |    }
                 |  },
                 |  "createdAt" : "2022-09-05T15:58:44.188Z",
                 |  "lastUpdated" : "2022-09-07T10:33:23.472Z",
                 |  "submissionStatus" : "notSubmitted"
                 |}
                 |""".stripMargin)

            val uA: UserAnswers = json.as[UserAnswers]

            val converted = TransitOperation.transform(uA, Phase.Phase5)

            val expected = TransitOperationType01(
              MRN = mrn,
              arrivalNotificationDateAndTime = converted.arrivalNotificationDateAndTime,
              simplifiedProcedure = Number0,
              incidentFlag = Some(Number0)
            )

            converted shouldEqual expected
          }

          "simplified procedure" in {

            val json: JsValue = Json.parse(s"""
                 |{
                 |  "_id" : "c8fdf8a7-1c77-4d25-991d-2a0881e05062",
                 |  "mrn" : "$mrn",
                 |  "eoriNumber" : "GB1234567",
                 |  "data" : {
                 |    "identification" : {
                 |      "destinationOffice" : {
                 |        "id" : "GB000051",
                 |        "name" : "Felixstowe",
                 |        "phoneNumber" : "+44 (0)1394 303023 / 24 / 26"
                 |      },
                 |      "identificationNumber" : "GB123456789000",
                 |      "isSimplifiedProcedure" : "simplified"
                 |    },
                 |    "locationOfGoods" : {
                 |      "typeOfLocation" : "authorisedPlace",
                 |      "qualifierOfIdentification" : "customsOffice",
                 |      "qualifierOfIdentificationDetails" : {
                 |        "customsOffice" : {
                 |          "id" : "GB000142",
                 |          "name" : "Belfast EPU",
                 |          "phoneNumber" : "+44 (0)3000 523068"
                 |        }
                 |      }
                 |    }
                 |  },
                 |  "createdAt" : "2022-09-05T15:58:44.188Z",
                 |  "lastUpdated" : "2022-09-07T10:33:23.472Z",
                 |  "submissionStatus" : "notSubmitted"
                 |}
                 |""".stripMargin)

            val uA: UserAnswers = json.as[UserAnswers]

            val converted = TransitOperation.transform(uA, Phase.Phase5)

            val expected = TransitOperationType01(
              MRN = mrn,
              arrivalNotificationDateAndTime = converted.arrivalNotificationDateAndTime,
              simplifiedProcedure = Number1,
              incidentFlag = Some(Number0)
            )

            converted shouldEqual expected
          }
        }

        "phase 6" in {

          val json: JsValue = Json.parse(s"""
               |{
               |  "_id" : "c8fdf8a7-1c77-4d25-991d-2a0881e05062",
               |  "mrn" : "$mrn",
               |  "eoriNumber" : "GB1234567",
               |  "data" : {
               |    "identification" : {
               |      "destinationOffice" : {
               |        "id" : "GB000051",
               |        "name" : "Felixstowe",
               |        "phoneNumber" : "+44 (0)1394 303023 / 24 / 26"
               |      },
               |      "identificationNumber" : "GB123456789000",
               |      "isSimplifiedProcedure" : "simplified"
               |    },
               |    "locationOfGoods" : {
               |      "typeOfLocation" : "authorisedPlace",
               |      "qualifierOfIdentification" : "customsOffice",
               |      "qualifierOfIdentificationDetails" : {
               |        "customsOffice" : {
               |          "id" : "GB000142",
               |          "name" : "Belfast EPU",
               |          "phoneNumber" : "+44 (0)3000 523068"
               |        }
               |      }
               |    }
               |  },
               |  "createdAt" : "2022-09-05T15:58:44.188Z",
               |  "lastUpdated" : "2022-09-07T10:33:23.472Z",
               |  "submissionStatus" : "notSubmitted"
               |}
               |""".stripMargin)

          val uA: UserAnswers = json.as[UserAnswers]

          val converted = TransitOperation.transform(uA, Phase.Phase6)

          val expected = TransitOperationType01(
            MRN = mrn,
            arrivalNotificationDateAndTime = converted.arrivalNotificationDateAndTime,
            simplifiedProcedure = Number1,
            incidentFlag = None
          )

          converted shouldEqual expected
        }
      }

      "throw exception" when {
        "unknown procedure" in {

          val json: JsValue = Json.parse(s"""
               |{
               |  "_id" : "c8fdf8a7-1c77-4d25-991d-2a0881e05062",
               |  "mrn" : "$mrn",
               |  "eoriNumber" : "GB1234567",
               |  "data" : {
               |    "identification" : {
               |      "destinationOffice" : {
               |        "id" : "GB000051",
               |        "name" : "Felixstowe",
               |        "phoneNumber" : "+44 (0)1394 303023 / 24 / 26"
               |      },
               |      "identificationNumber" : "GB123456789000",
               |      "isSimplifiedProcedure" : "foo"
               |    },
               |    "locationOfGoods" : {
               |      "typeOfLocation" : "authorisedPlace",
               |      "qualifierOfIdentification" : "customsOffice",
               |      "qualifierOfIdentificationDetails" : {
               |        "customsOffice" : {
               |          "id" : "GB000142",
               |          "name" : "Belfast EPU",
               |          "phoneNumber" : "+44 (0)3000 523068"
               |        }
               |      }
               |    }
               |  },
               |  "createdAt" : "2022-09-05T15:58:44.188Z",
               |  "lastUpdated" : "2022-09-07T10:33:23.472Z",
               |  "submissionStatus" : "notSubmitted"
               |}
               |""".stripMargin)

          val uA: UserAnswers = json.as[UserAnswers]

          an[Exception] should be thrownBy TransitOperation.transform(uA, Phase.Phase5)
        }

        "undefined procedure" in {

          val json: JsValue = Json.parse(s"""
               |{
               |  "_id" : "c8fdf8a7-1c77-4d25-991d-2a0881e05062",
               |  "mrn" : "$mrn",
               |  "eoriNumber" : "GB1234567",
               |  "data" : {
               |    "identification" : {
               |      "destinationOffice" : {
               |        "id" : "GB000051",
               |        "name" : "Felixstowe",
               |        "phoneNumber" : "+44 (0)1394 303023 / 24 / 26"
               |      },
               |      "identificationNumber" : "GB123456789000"
               |    },
               |    "locationOfGoods" : {
               |      "typeOfLocation" : "authorisedPlace",
               |      "qualifierOfIdentification" : "customsOffice",
               |      "qualifierOfIdentificationDetails" : {
               |        "customsOffice" : {
               |          "id" : "GB000142",
               |          "name" : "Belfast EPU",
               |          "phoneNumber" : "+44 (0)3000 523068"
               |        }
               |      }
               |    }
               |  },
               |  "createdAt" : "2022-09-05T15:58:44.188Z",
               |  "lastUpdated" : "2022-09-07T10:33:23.472Z",
               |  "submissionStatus" : "notSubmitted"
               |}
               |""".stripMargin)

          val uA: UserAnswers = json.as[UserAnswers]

          an[Exception] should be thrownBy TransitOperation.transform(uA, Phase.Phase5)
        }
      }
    }
  }
}
