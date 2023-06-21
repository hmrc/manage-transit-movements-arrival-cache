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

import generated.TransitOperationType02
import models.UserAnswers
import play.api.libs.functional.syntax._
import play.api.libs.json.{__, Reads}

import java.time.LocalDateTime

object TransitOperation {

  def transform(uA: UserAnswers): TransitOperationType02 =
    uA.metadata.data.as[TransitOperationType02](transitOperationType02.reads(uA.mrn))
}

object transitOperationType02 {

  val isSimplifiedReader: Reads[Boolean] = (identificationPath \ "isSimplifiedProcedure").read[String].map(_.equals("simplified"))

  def reads(mrn: String): Reads[TransitOperationType02] = (
    isSimplifiedReader and
      (__ \ "incidentFlag").readWithDefault[Boolean](false)
  ).apply {
    (isSimplified, isIncident) =>
      TransitOperationType02(
        MRN = mrn,
        arrivalNotificationDateAndTime = LocalDateTime.now(),
        simplifiedProcedure = isSimplified,
        incidentFlag = isIncident
      )
  }
}
