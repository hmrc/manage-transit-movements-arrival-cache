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

import generated.*
import models.UserAnswers
import play.api.libs.functional.syntax.*
import play.api.libs.json.{__, Reads}

object Consignment {

  def transform(uA: UserAnswers): ConsignmentType01 =
    uA.metadata.data.as[ConsignmentType01](consignmentType01.reads)

}

object consignmentType01 {

  implicit val reads: Reads[ConsignmentType01] =
    (__ \ "locationOfGoods").read[LocationOfGoodsType01](locationOfGoodsType01.reads).map(ConsignmentType01(_))
}

object locationOfGoodsType01 {

  // If procedure type is simple then we automatically set typeOfLocation to B and qualifierOfIdentification to Y (CTCP-2666)
  implicit val reads: Reads[LocationOfGoodsType01] =
    (
      (__ \ "typeOfLocation" \ "type").readWithDefault[String]("B") and
        (__ \ "qualifierOfIdentification" \ "qualifier").readWithDefault[String]("Y") and
        (__ \ "qualifierOfIdentificationDetails" \ "authorisationNumber").readNullable[String] and
        (__ \ "qualifierOfIdentificationDetails" \ "additionalIdentifier").readNullable[String] and
        (__ \ "qualifierOfIdentificationDetails" \ "unlocode").readNullable[String] and
        (__ \ "qualifierOfIdentificationDetails" \ "customsOffice").readNullable[CustomsOfficeType01](customsOfficeType01.reads) and
        (__ \ "qualifierOfIdentificationDetails" \ "coordinates").readNullable[GNSSType](gnssType.reads) and
        (__ \ "qualifierOfIdentificationDetails" \ "identificationNumber").readNullable[EconomicOperatorType02](economicOperatorType02.reads) and
        (__ \ "qualifierOfIdentificationDetails").read[Option[AddressType06]](addressType06.reads) and
        Reads.pure[Option[PostcodeAddressType]](None) and
        (__ \ "contactPerson").readNullable[ContactPersonType01](contactPersonType01.reads)
    )(LocationOfGoodsType01.apply)
}

object economicOperatorType02 {

  implicit val reads: Reads[EconomicOperatorType02] =
    __.read[String].map(EconomicOperatorType02.apply)
}

object customsOfficeType01 {

  implicit val reads: Reads[CustomsOfficeType01] =
    (__ \ "id").read[String].map(CustomsOfficeType01.apply)
}

object gnssType {

  implicit val reads: Reads[GNSSType] = (
    (__ \ "latitude").read[String] and
      (__ \ "longitude").read[String]
  )(GNSSType.apply)
}

object addressType06 {

  implicit val reads: Reads[Option[AddressType06]] = (
    (__ \ "address" \ "numberAndStreet").readNullable[String] and
      (__ \ "address" \ "postalCode").readNullable[String] and
      (__ \ "address" \ "city").readNullable[String] and
      (__ \ "country" \ "code").readNullable[String]
  ).tupled.map {
    case (Some(streetAndNumber), postcode, Some(city), Some(country)) =>
      Some(AddressType06(streetAndNumber, postcode, city, country))
    case _ => None
  }
}

object contactPersonType01 {

  implicit val reads: Reads[ContactPersonType01] = (
    (__ \ "name").read[String] and
      (__ \ "telephoneNumber").read[String] and
      Reads.pure[Option[String]](None)
  )(ContactPersonType01.apply)
}
