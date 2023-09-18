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
import generated._
import models.UserAnswers
import play.api.libs.json.{JsValue, Json}

class AuthorisationsSpec extends SpecBase {

  "Authorisations" when {

    "transform is called" should {

      "convert to API format" in {

        val json: JsValue = Json.parse(s"""
             |{
             |  "_id" : "c8fdf8a7-1c77-4d25-991d-2a0881e05062",
             |  "mrn" : "$mrn",
             |  "eoriNumber" : "GB1234567",
             |  "isSubmitted" : "notSubmitted",
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
             |      "typeOfLocation" : "authorisedPlace",
             |      "qualifierOfIdentification" : "customsOffice",
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
             |          "code" : "GB",
             |          "description" : "United Kingdom"
             |        },
             |        "incidentCode" : "partiallyOrFullyUnloaded",
             |        "incidentText" : "foo",
             |        "addEndorsement" : true,
             |        "endorsement" : {
             |          "date" : "2023-01-01",
             |          "authority" : "bar",
             |          "country" : {
             |            "code" : "GB",
             |            "description" : "United Kingdom"
             |          },
             |          "location" : "foobar"
             |        },
             |        "qualifierOfIdentification" : "unlocode",
             |        "unLocode" : "ADCAN",
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
             |  "lastUpdated" : {
             |    "$$date" : {
             |      "$$numberLong" : "1662546803472"
             |    }
             |  },
             |  "createdAt" : {
             |    "$$date" : {
             |      "$$numberLong" : "1662546803472"
             |    }
             |  }
             |}
             |""".stripMargin)

        val uA: UserAnswers = json.as[UserAnswers](UserAnswers.mongoFormat)

        val converted = Authorisations.transform(uA)

        val expected = Seq(
          AuthorisationType01(
            sequenceNumber = "1",
            typeValue = "C522",
            referenceNumber = "SSE1"
          )
        )

        converted shouldBe expected
      }

      "Authorization Type should be ACE when ProcedureType is Simplified and should convert to API format" in {

        val json: JsValue = Json.parse(s"""
             |{
             |  "_id" : "c8fdf8a7-1c77-4d25-991d-2a0881e05062",
             |  "mrn" : "$mrn",
             |  "eoriNumber" : "GB1234567",
             |  "isSubmitted" : "notSubmitted",
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
             |      "typeOfLocation" : "authorisedPlace",
             |      "qualifierOfIdentification" : "customsOffice",
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
             |          "code" : "GB",
             |          "description" : "United Kingdom"
             |        },
             |        "incidentCode" : "partiallyOrFullyUnloaded",
             |        "incidentText" : "foo",
             |        "addEndorsement" : true,
             |        "endorsement" : {
             |          "date" : "2023-01-01",
             |          "authority" : "bar",
             |          "country" : {
             |            "code" : "GB",
             |            "description" : "United Kingdom"
             |          },
             |          "location" : "foobar"
             |        },
             |        "qualifierOfIdentification" : "unlocode",
             |        "unLocode" : "ADCAN",
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
             |  "lastUpdated" : {
             |    "$$date" : {
             |      "$$numberLong" : "1662546803472"
             |    }
             |  },
             |  "createdAt" : {
             |    "$$date" : {
             |      "$$numberLong" : "1662546803472"
             |    }
             |  }
             |}
             |""".stripMargin)

        val uA: UserAnswers = json.as[UserAnswers](UserAnswers.mongoFormat)

        val converted = Authorisations.transform(uA)

        val expected = Seq(
          AuthorisationType01(
            sequenceNumber = "1",
            typeValue = "C522",
            referenceNumber = "SSE1"
          )
        )

        converted shouldBe expected
      }

      "Authorization Type should Not be set when ProcedureType is Normal and should convert to API format" in {

        val json: JsValue = Json.parse(s"""
             |{
             |  "_id" : "c8fdf8a7-1c77-4d25-991d-2a0881e05062",
             |  "mrn" : "$mrn",
             |  "eoriNumber" : "GB1234567",
             |  "isSubmitted" : "notSubmitted",
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
             |    },
             |    "incidentFlag" : true,
             |    "incidents" : [
             |      {
             |        "incidentCountry" : {
             |          "code" : "GB",
             |          "description" : "United Kingdom"
             |        },
             |        "incidentCode" : "partiallyOrFullyUnloaded",
             |        "incidentText" : "foo",
             |        "addEndorsement" : true,
             |        "endorsement" : {
             |          "date" : "2023-01-01",
             |          "authority" : "bar",
             |          "country" : {
             |            "code" : "GB",
             |            "description" : "United Kingdom"
             |          },
             |          "location" : "foobar"
             |        },
             |        "qualifierOfIdentification" : "unlocode",
             |        "unLocode" : "ADCAN",
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
             |  "lastUpdated" : {
             |    "$$date" : {
             |      "$$numberLong" : "1662546803472"
             |    }
             |  },
             |  "createdAt" : {
             |    "$$date" : {
             |      "$$numberLong" : "1662546803472"
             |    }
             |  }
             |}
             |""".stripMargin)

        val uA: UserAnswers = json.as[UserAnswers](UserAnswers.mongoFormat)

        val converted = Authorisations.transform(uA)

        val expected = List.empty

        converted shouldBe expected
      }

    }
  }
}
