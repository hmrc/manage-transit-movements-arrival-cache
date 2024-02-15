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

import generated.{CC007C, CC007CType, MESSAGESequence, PhaseIDtype}
import models.UserAnswers
import scalaxb.DataRecord
import scalaxb.`package`.toXML

import javax.inject.Inject
import scala.xml.{NamespaceBinding, NodeSeq}

class Declaration @Inject() (header: Header) {

  private val scope: NamespaceBinding = scalaxb.toScope(Some("ncts") -> "http://ncts.dgtaxud.ec")

  def transform(userAnswers: UserAnswers): NodeSeq =
    toXML(IE007(userAnswers), s"ncts:${CC007C.toString}", scope)

  private def IE007(userAnswers: UserAnswers): CC007CType = {
    val message: MESSAGESequence   = header.message(userAnswers)
    val transitOperation           = TransitOperation.transform(userAnswers)
    val authorisations             = Authorisations.transform(userAnswers)
    val customsOfficeOfDestination = DestinationDetails.customsOfficeOfDestination(userAnswers)
    val traderAtDestination        = DestinationDetails.traderAtDestination(userAnswers)
    val consignment                = Consignment.transform(userAnswers)

    CC007CType(
      messageSequence1 = message,
      TransitOperation = transitOperation,
      Authorisation = authorisations,
      CustomsOfficeOfDestinationActual = customsOfficeOfDestination,
      TraderAtDestination = traderAtDestination,
      Consignment = consignment,
      attributes = Map("@PhaseID" -> DataRecord(PhaseIDtype.fromString("NCTS5.0", scope)))
    )
  }
}
