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

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Metadata(
  mrn: String,
  eoriNumber: String,
  data: JsObject,
  submissionStatus: SubmissionStatus
)

object Metadata {

  implicit val nonSensitiveReads: Reads[Metadata] = Json.reads[Metadata]

  implicit val nonSensitiveWrites: Writes[Metadata] = Json.writes[Metadata]

  def sensitiveReads(implicit sensitiveFormats: SensitiveFormats): Reads[Metadata] =
    (
      (__ \ "mrn").read[String] and
        (__ \ "eoriNumber").read[String] and
        (__ \ "data").read[JsObject](sensitiveFormats.jsObjectReads) and
        (__ \ "submissionStatus").read[SubmissionStatus]
    )(Metadata.apply)

  def sensitiveWrites(implicit sensitiveFormats: SensitiveFormats): Writes[Metadata] =
    (
      (__ \ "mrn").write[String] and
        (__ \ "eoriNumber").write[String] and
        (__ \ "data").write[JsObject](sensitiveFormats.jsObjectWrites) and
        (__ \ "submissionStatus").write[SubmissionStatus]
    )(
      md => Tuple.fromProductTyped(md)
    )
}
