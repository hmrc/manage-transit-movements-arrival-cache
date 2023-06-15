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

import api.submission.transitOperationType02.isSimplifiedReader
import generated.AuthorisationType01
import models.UserAnswers
import play.api.libs.json.Reads

object Authorisations {

  def transform(uA: UserAnswers): Seq[AuthorisationType01] = uA.metadata.data.as[Seq[AuthorisationType01]](authorisationType01.reads)
}

object authorisationType01 {

  private def convertAuthType(referenceNumber: String): Seq[AuthorisationType01] =
    Seq(AuthorisationType01("1", "ACE", referenceNumber))

  // Auth Type is always set to ACE - refer - CTCP-3227
  def reads: Reads[Seq[AuthorisationType01]] =
    isSimplifiedReader flatMap {
      case true =>
        (authorisationsPath \ "referenceNumber")
          .read[String]
          .map(
            referenceNumber => convertAuthType(referenceNumber)
          )

      case _ =>
        (authorisationsPath \ "referenceNumber")
          .readNullable[String]
          .map {
            case Some(referenceNumber) => convertAuthType(referenceNumber)
            case None                  => Nil
          }
    }

}
