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

package models

import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import java.util.UUID

final case class UserAnswers(
  metadata: Metadata,
  createdAt: Instant,
  lastUpdated: Instant,
  id: UUID
) {

  val mrn: String        = metadata.mrn
  val eoriNumber: String = metadata.eoriNumber

  def get[A](path: JsPath)(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(path)).reads(metadata.data).getOrElse(None)
}

object UserAnswers {

  import play.api.libs.functional.syntax._

  implicit val nonSensitiveFormat: Format[UserAnswers] =
    Format(
      reads(implicitly, implicitly),
      writes(implicitly, implicitly)
    )

  def sensitiveFormat(implicit sensitiveFormats: SensitiveFormats): Format[UserAnswers] =
    Format(
      reads(MongoJavatimeFormats.instantReads, Metadata.sensitiveReads),
      writes(MongoJavatimeFormats.instantWrites, Metadata.sensitiveWrites)
    )

  private def reads(implicit instantReads: Reads[Instant], metaDataReads: Reads[Metadata]): Reads[UserAnswers] =
    (
      __.read[Metadata] and
        (__ \ "createdAt").read[Instant] and
        (__ \ "lastUpdated").read[Instant] and
        (__ \ "_id").read[UUID]
    )(UserAnswers.apply)

  private def writes(implicit instantWrites: Writes[Instant], metaDataWrites: Writes[Metadata]): Writes[UserAnswers] =
    (
      __.write[Metadata] and
        (__ \ "createdAt").write[Instant] and
        (__ \ "lastUpdated").write[Instant] and
        (__ \ "_id").write[UUID]
    )(
      ua => Tuple.fromProductTyped(ua)
    )

}
