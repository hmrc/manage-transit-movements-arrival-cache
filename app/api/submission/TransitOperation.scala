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
import models.{Phase, UserAnswers}
import play.api.libs.json.Reads

import java.time.LocalDateTime

object TransitOperation {

  def transform(uA: UserAnswers, version: Phase): TransitOperationType01 =
    uA.metadata.data.as[TransitOperationType01](transitOperationType01.reads(uA.mrn, version))
}

object transitOperationType01 {

  val isSimplifiedReader: Reads[Boolean] = (identificationPath \ "isSimplifiedProcedure").read[String].map {
    case "simplified" => true
    case "normal"     => false
    case x            => throw new Exception(s"Invalid procedure type value: $x")
  }

  def reads(mrn: String, version: Phase): Reads[TransitOperationType01] =
    isSimplifiedReader.map {
      isSimplified =>
        TransitOperationType01(
          MRN = mrn,
          arrivalNotificationDateAndTime = LocalDateTime.now(),
          simplifiedProcedure = isSimplified,
          incidentFlag = version match
            case Phase.Phase5 => Some(Number0)
            case Phase.Phase6 => None
        )
    }
}
