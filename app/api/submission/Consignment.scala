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

import generated.{EndorsementType01, _}
import models.UserAnswers
import play.api.libs.functional.syntax._
import play.api.libs.json.{__, Reads}

object Consignment {

  def transform(uA: UserAnswers): ConsignmentType01 =
    uA.metadata.data.as[ConsignmentType01](consignmentType01.reads)

}

object consignmentType01 {

  implicit val reads: Reads[ConsignmentType01] = (
    (__ \ "locationOfGoods").read[LocationOfGoodsType01](locationOfGoodsType01.reads) and
      (__ \ "incidents").readArray[IncidentType01](incidentType01.reads)
  )(ConsignmentType01.apply)
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
        (__ \ "qualifierOfIdentificationDetails" \ "identificationNumber").readNullable[EconomicOperatorType03](economicOperatorType03.reads) and
        (__ \ "qualifierOfIdentificationDetails").read[Option[AddressType14]](addressType14.reads) and
        (__ \ "qualifierOfIdentificationDetails" \ "postalCode").readNullable[PostcodeAddressType02](postcodeAddressType02.reads) and
        (__ \ "contactPerson").readNullable[ContactPersonType06](contactPersonType06.reads)
    )(LocationOfGoodsType01.apply)
}

object economicOperatorType03 {

  implicit val reads: Reads[EconomicOperatorType03] =
    __.read[String].map(EconomicOperatorType03.apply)
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

object addressType14 {

  implicit val reads: Reads[Option[AddressType14]] = (
    (__ \ "address" \ "numberAndStreet").readNullable[String] and
      (__ \ "address" \ "postalCode").readNullable[String] and
      (__ \ "address" \ "city").readNullable[String] and
      (__ \ "country" \ "code").readNullable[String]
  ).tupled.map {
    case (Some(streetAndNumber), postcode, Some(city), Some(country)) =>
      Some(AddressType14(streetAndNumber, postcode, city, country))
    case _ => None
  }

}

object addressType01 {

  implicit val reads: Reads[AddressType01] = (
    (__ \ "numberAndStreet").read[String] and
      (__ \ "postalCode").readNullable[String] and
      (__ \ "city").read[String]
  )(AddressType01.apply)

}

object postcodeAddressType02 {

  implicit val reads: Reads[PostcodeAddressType02] = (
    (__ \ "streetNumber").readNullable[String] and
      (__ \ "postalCode").read[String] and
      (__ \ "country" \ "code").read[String]
  )(PostcodeAddressType02.apply)

}

object contactPersonType06 {

  implicit val reads: Reads[ContactPersonType06] = (
    (__ \ "name").read[String] and
      (__ \ "telephoneNumber").read[String] and
      Reads.pure[Option[String]](None)
  )(ContactPersonType06.apply)

}

object incidentType01 {

  def reads(index: Int): Reads[IncidentType01] = (
    Reads.pure[BigInt](index) and
      (__ \ "incidentCode" \ "code").read[String] and
      (__ \ "incidentText").read[String] and
      (__ \ "endorsement").readNullable[EndorsementType01](endorsementType01.reads) and
      __.read[LocationType01](locationType01.reads) and
      (__ \ "equipments").readArray[TransportEquipmentType01](transportEquipmentType01.reads) and
      __.read[Option[TranshipmentType01]](transhipmentType01.reads)
  )(IncidentType01.apply)

}

object endorsementType01 {

  def reads: Reads[EndorsementType01] = (
    (__ \ "date").read[String].map(stringToXMLGregorianCalendar) and
      (__ \ "authority").read[String] and
      (__ \ "location").read[String] and
      (__ \ "country" \ "code").read[String]
  )(EndorsementType01.apply)

}

object locationType01 {

  def reads: Reads[LocationType01] = (
    (__ \ "qualifierOfIdentification" \ "qualifier").read[String] and
      (__ \ "unLocode").readNullable[String] and
      (__ \ "incidentCountry" \ "code").read[String] and
      (__ \ "coordinates").readNullable[GNSSType](gnssType.reads) and
      (__ \ "address").readNullable[AddressType01](addressType01.reads)
  )(LocationType01.apply)

}

object transportEquipmentType01 {

  def apply(
    sequenceNumber: BigInt,
    containerIdentificationNumber: Option[String],
    Seal: Seq[SealType05],
    GoodsReference: Seq[GoodsReferenceType01]
  ): TransportEquipmentType01 =
    TransportEquipmentType01(sequenceNumber, containerIdentificationNumber, Some(Seal.length), Seal, GoodsReference)

  def reads(index: Int): Reads[TransportEquipmentType01] = (
    Reads.pure[BigInt](index) and
      (__ \ "containerIdentificationNumber").readNullable[String] and
      (__ \ "seals").readArray[SealType05](sealType05.reads) and
      (__ \ "itemNumbers").readArray[GoodsReferenceType01](goodsReferenceType01.reads)
  )(transportEquipmentType01.apply)

}

object sealType05 {

  def reads(index: Int): Reads[SealType05] = (
    Reads.pure[BigInt](index) and
      (__ \ "sealIdentificationNumber").read[String]
  )(SealType05.apply)

}

object goodsReferenceType01 {

  def reads(index: Int): Reads[GoodsReferenceType01] = (
    Reads.pure[BigInt](index) and
      (__ \ "itemNumber").read[String].map(BigInt(_))
  )(GoodsReferenceType01.apply)

}

object transhipmentType01 {

  def reads: Reads[Option[TranshipmentType01]] = (
    (__ \ "containerIndicatorYesNo").readWithDefault[Boolean](false) and
      (__ \ "transportMeans").readNullable[TransportMeansType01](transportMeansType01.reads)
  ).apply {
    (containerIndicator, transportMeans) =>
      transportMeans.map(
        x =>
          TranshipmentType01(
            containerIndicator = containerIndicator,
            TransportMeans = x
          )
      )
  }

}

object transportMeansType01 {

  def reads: Reads[TransportMeansType01] = (
    (__ \ "identification" \ "type").read[String] and
      (__ \ "identificationNumber").read[String] and
      (__ \ "transportNationality" \ "code").read[String]
  )(TransportMeansType01.apply)

}
