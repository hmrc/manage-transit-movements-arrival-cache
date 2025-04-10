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
import play.api.libs.json.Reads

import java.time.LocalDateTime

object TransitOperation {

  def transform(uA: UserAnswers): TransitOperationType01 =
    uA.metadata.data.as[TransitOperationType01](transitOperationType01.reads(uA.mrn))
}

object transitOperationType01 {

  val isSimplifiedReader: Reads[Boolean] = (identificationPath \ "isSimplifiedProcedure").read[String].map {
    case "simplified" => true
    case "normal"     => false
    case x            => throw new Exception(s"Invalid procedure type value: $x")
  }

  // TODO - feature flag driven by Accept header
  //  phase 5 -> incidentFlag = Some(Number0)
  //  phase 6 -> incidentFlag = None
  def reads(mrn: String): Reads[TransitOperationType01] =
    isSimplifiedReader.map {
      isSimplified =>
        TransitOperationType01(
          MRN = mrn,
          arrivalNotificationDateAndTime = LocalDateTime.now(),
          simplifiedProcedure = isSimplified,
          incidentFlag = Some(Number0)
        )
    }
}
