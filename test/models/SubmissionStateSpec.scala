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

import base.SpecBase
import play.api.libs.json.{JsString, Json}

class SubmissionStateSpec extends SpecBase {

  "must serialise to json" when {
    "Submitted" in {
      val submissionState = SubmissionState.Submitted
      val result          = Json.toJson(submissionState)
      result shouldBe JsString("submitted")
    }

    "NotSubmitted" in {
      val submissionState = SubmissionState.NotSubmitted
      val result          = Json.toJson(submissionState)
      result shouldBe JsString("notSubmitted")
    }

    "RejectedPendingChanges" in {
      val submissionState = SubmissionState.RejectedPendingChanges
      val result          = Json.toJson(submissionState)
      result shouldBe JsString("rejectedPendingChanges")
    }
  }

  "must deserialise from json" when {
    "Completed" in {
      val json   = JsString("submitted")
      val result = json.as[SubmissionState.Value]
      result shouldBe SubmissionState.Submitted
    }

    "NotSubmitted" in {
      val json   = JsString("notSubmitted")
      val result = json.as[SubmissionState.Value]
      result shouldBe SubmissionState.NotSubmitted
    }

    "RejectedPendingChanges" in {
      val json   = JsString("rejectedPendingChanges")
      val result = json.as[SubmissionState.Value]
      result shouldBe SubmissionState.RejectedPendingChanges
    }
  }

}
