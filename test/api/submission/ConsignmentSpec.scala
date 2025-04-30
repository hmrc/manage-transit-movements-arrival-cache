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
import generated._
import models.UserAnswers
import play.api.libs.json.{JsValue, Json}

class ConsignmentSpec extends SpecBase with AppWithDefaultMockFixtures {

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
                 |    },
                 |    "incidentFlag" : true,
                 |    "incidents" : [
                 |      {
                 |        "incidentCountry" : {
                 |          "code" : "GB",
                 |          "description" : "United Kingdom"
                 |        },
                 |        "incidentCode" : {
                 |          "code": "4",
                 |          "description": "Imminent danger necessitates immediate partial or total unloading of the sealed means of transport."
                 |        },
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
                 |        "qualifierOfIdentification" : {
                 |          "qualifier": "U",
                 |          "description": "UN/LOCODE"
                 |        },
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
                 |        ],
                 |        "transportMeans" : {
                 |          "identification" : {
                 |            "type": "11",
                 |            "description": "Name of the sea-going vessel"
                 |          },
                 |          "identificationNumber" : "foo",
                 |          "transportNationality" : {
                 |            "code" : "FR",
                 |            "desc" : "France"
                 |          }
                 |        }
                 |      }
                 |    ]
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
                ),
                Incident = Seq(
                  IncidentType01(
                    sequenceNumber = 1,
                    code = "4",
                    text = "foo",
                    Endorsement = Some(
                      EndorsementType01(
                        date = converted.Incident
                          .flatMap(
                            x =>
                              x.Endorsement.map(
                                y => y.date
                              )
                          )
                          .head,
                        authority = "bar",
                        place = "foobar",
                        country = "GB"
                      )
                    ),
                    Location = LocationType01(
                      qualifierOfIdentification = "U",
                      UNLocode = Some("ADCAN"),
                      country = "GB",
                      GNSS = None,
                      Address = None
                    ),
                    TransportEquipment = Seq(
                      TransportEquipmentType01(
                        sequenceNumber = 1,
                        containerIdentificationNumber = Some("1"),
                        numberOfSeals = Some(BigInt(1)),
                        Seal = Seq(
                          SealType05(sequenceNumber = 1, identifier = "1")
                        ),
                        GoodsReference = Seq(
                          GoodsReferenceType01(
                            sequenceNumber = 1,
                            declarationGoodsItemNumber = BigInt(1)
                          )
                        )
                      )
                    ),
                    Transhipment = Some(
                      TranshipmentType01(
                        containerIndicator = Number0,
                        TransportMeans = TransportMeansType01(
                          typeOfIdentification = "11",
                          identificationNumber = "foo",
                          nationality = "FR"
                        )
                      )
                    )
                  )
                )
              )

              converted shouldBe expected

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
               |    },
               |    "incidentFlag" : true,
               |    "incidents" : [
               |      {
               |        "incidentCountry" : {
               |          "code" : "GB",
               |          "description" : "United Kingdom"
               |        },
               |        "incidentCode" : {
               |          "code": "4",
               |          "description": "Imminent danger necessitates immediate partial or total unloading of the sealed means of transport."
               |        },
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
               |        "qualifierOfIdentification" : {
               |          "qualifier": "U",
               |          "description": "UN/LOCODE"
               |        },
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
            ),
            Incident = Seq(
              IncidentType01(
                sequenceNumber = 1,
                code = "4",
                text = "foo",
                Endorsement = Some(
                  EndorsementType01(
                    date = converted.Incident
                      .flatMap(
                        x =>
                          x.Endorsement.map(
                            y => y.date
                          )
                      )
                      .head,
                    authority = "bar",
                    place = "foobar",
                    country = "GB"
                  )
                ),
                Location = LocationType01(
                  qualifierOfIdentification = "U",
                  UNLocode = Some("ADCAN"),
                  country = "GB",
                  GNSS = None,
                  Address = None
                ),
                TransportEquipment = Seq(
                  TransportEquipmentType01(
                    sequenceNumber = 1,
                    containerIdentificationNumber = Some("1"),
                    numberOfSeals = Some(BigInt(1)),
                    Seal = Seq(
                      SealType05(sequenceNumber = 1, identifier = "1")
                    ),
                    GoodsReference = Seq(
                      GoodsReferenceType01(
                        sequenceNumber = 1,
                        declarationGoodsItemNumber = BigInt(1)
                      )
                    )
                  )
                ),
                Transhipment = None
              )
            )
          )

          converted shouldBe expected

        }
      }
    }
  }
}
