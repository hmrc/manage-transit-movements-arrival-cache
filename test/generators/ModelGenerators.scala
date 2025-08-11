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

package generators

import generated.*
import models.{Phase, SubmissionStatus}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import scalaxb.XMLCalendar

import javax.xml.datatype.XMLGregorianCalendar

trait ModelGenerators {

  implicit lazy val arbitrarySubmissionState: Arbitrary[SubmissionStatus] = Arbitrary {
    val values = Seq(
      SubmissionStatus.NotSubmitted,
      SubmissionStatus.Submitted,
      SubmissionStatus.Amending
    )
    Gen.oneOf(values)
  }

  implicit lazy val arbitraryVersion: Arbitrary[Phase] =
    Arbitrary {
      Gen.oneOf(Phase.Phase5, Phase.Phase6)
    }

  implicit lazy val arbitraryMessageSequence: Arbitrary[MESSAGESequence] =
    Arbitrary {
      for {
        messageSender          <- Gen.alphaNumStr
        messageRecipient       <- Gen.alphaNumStr
        preparationDateAndTime <- arbitrary[XMLGregorianCalendar]
        messageIdentification  <- Gen.alphaNumStr
        correlationIdentifier  <- Gen.option(Gen.alphaNumStr)
      } yield MESSAGESequence(
        messageSender = messageSender,
        messageRecipient = messageRecipient,
        preparationDateAndTime = preparationDateAndTime,
        messageIdentification = messageIdentification,
        messageType = CC007C,
        correlationIdentifier = correlationIdentifier
      )
    }

  implicit lazy val arbitraryXMLGregorianCalendar: Arbitrary[XMLGregorianCalendar] =
    Arbitrary {
      Gen.const(XMLCalendar("2025-08-11"))
    }
}
