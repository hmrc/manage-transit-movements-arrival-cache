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

sealed trait SubmissionStatus {
  val asString: String
}

object SubmissionStatus {

  case object NotSubmitted extends SubmissionStatus {
    override val asString: String = "notSubmitted"
  }

  case object Submitted extends SubmissionStatus {
    override val asString: String = "submitted"
  }

  case object Amending extends SubmissionStatus {
    override val asString: String = "amending"
  }

  implicit val reads: Reads[SubmissionStatus] = Reads {
    case JsString(NotSubmitted.asString) => JsSuccess(NotSubmitted)
    case JsString(Submitted.asString)    => JsSuccess(Submitted)
    case JsString(Amending.asString)     => JsSuccess(Amending)
    case x                               => JsError(s"Could not read $x as SubmissionStatus")
  }

  implicit val writes: Writes[SubmissionStatus] = Writes {
    state => JsString(state.asString)
  }
}
