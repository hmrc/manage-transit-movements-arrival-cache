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
import models.UserAnswers
import play.api.libs.json.{JsValue, Json}

class ConsignmentSpec extends SpecBase {

  "Consignment" when {

    "transform is called" when {

      "isSimplified is true" should {
        "automatically set type of location to B " +
          "and qualifierOfIdentification to Y" when {
            "when converting to an API format" in {
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
                 |      "isSimplifiedProcedure" : "simplified",
                 |      "authorisationReferenceNumber" : "SSE1"
                 |    },
                 |    "locationOfGoods" : {
                 |      "qualifierOfIdentificationDetails" : {
                 |        "authorisationNumber" : "GB123456789000",
                 |        "addAdditionalIdentifier" : true,
                 |        "additionalIdentifier" : "0000"
                 |      }
                 |    }
                 |  },
                 |  "createdAt" : "2022-09-05T15:58:44.188Z",
                 |  "lastUpdated" : "2022-09-07T10:33:23.472Z",
                 |  "submissionStatus" : "notSubmitted"
                 |}
                 |""".stripMargin)

              val uA: UserAnswers = json.as[UserAnswers]

              val converted = Consignment.transform(uA)

              val expected = ConsignmentType01(
                LocationOfGoods = LocationOfGoodsType01(
                  typeOfLocation = "B",
                  qualifierOfIdentification = "Y",
                  authorisationNumber = Some("GB123456789000"),
                  additionalIdentifier = Some("0000"),
                  UNLocode = None,
                  CustomsOffice = None,
                  GNSS = None,
                  EconomicOperator = None,
                  Address = None,
                  PostcodeAddress = None,
                  ContactPerson = None
                )
              )

              converted shouldEqual expected

            }
          }
      }

      "isSimplified is false" should {
        "convert to API format" in {

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
               |      "isSimplifiedProcedure" : "normal",
               |      "authorisationReferenceNumber" : "SSE1"
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
               |    }
               |  },
               |  "createdAt" : "2022-09-05T15:58:44.188Z",
               |  "lastUpdated" : "2022-09-07T10:33:23.472Z",
               |  "submissionStatus" : "notSubmitted"
               |}
               |""".stripMargin)

          val uA: UserAnswers = json.as[UserAnswers]

          val converted = Consignment.transform(uA)

          val expected = ConsignmentType01(
            LocationOfGoods = LocationOfGoodsType01(
              typeOfLocation = "B",
              qualifierOfIdentification = "V",
              authorisationNumber = None,
              additionalIdentifier = None,
              UNLocode = None,
              CustomsOffice = Some(CustomsOfficeType01("GB000142")),
              GNSS = None,
              EconomicOperator = None,
              Address = None,
              PostcodeAddress = None,
              ContactPerson = None
            )
          )

          converted shouldEqual expected

        }
      }
    }
  }
}
