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

import play.api.libs.json.{JsObject, Json}

import java.time.Instant

case class UserAnswersSummary(eoriNumber: String, userAnswers: Seq[UserAnswers], totalMovements: Int, totalMatchingMovements: Int) {

  def toHateoas(expiresInDays: Instant => Long): JsObject =
    Json.obj(
      "eoriNumber"             -> eoriNumber,
      "totalMovements"         -> totalMovements,
      "totalMatchingMovements" -> totalMatchingMovements,
      "userAnswers" -> userAnswers.map {
        userAnswer =>
          Json.obj(
            "mrn" -> userAnswer.mrn,
            "_links" -> Json.obj(
              "self" -> Json.obj("href" -> controllers.routes.CacheController.get(userAnswer.mrn).url)
            ),
            "createdAt"     -> userAnswer.createdAt,
            "lastUpdated"   -> userAnswer.lastUpdated,
            "expiresInDays" -> expiresInDays(userAnswer.createdAt),
            "_id"           -> userAnswer.id
          )
      }
    )

}
