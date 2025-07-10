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
import generators.Generators
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsError, JsString, Json}

class SubmissionStatusSpec extends SpecBase with Generators {

  "SubmissionStatus" when {

    "reads" should {
      "deserialise" when {
        "valid status" in {
          forAll(arbitrary[SubmissionStatus]) {
            state =>
              JsString(state.asString).as[SubmissionStatus] shouldEqual state
          }
        }
      }

      "fail to deserialise" when {
        "invalid status" in {
          JsString("foo").validate[SubmissionStatus] shouldBe a[JsError]
        }
      }
    }

    "writes" should {
      "serialise" in {
        forAll(arbitrary[SubmissionStatus]) {
          state =>
            Json.toJson(state) shouldEqual JsString(state.asString)
        }
      }
    }
  }
}
