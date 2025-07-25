/*
 * Copyright 2024 HM Revenue & Customs
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

package services

import api.submission.Declaration
import cats.implicits.toTraverseOps
import connectors.ApiConnector
import models.{Messages, Phase, UserAnswers}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ApiService @Inject() (
  apiConnector: ApiConnector,
  declaration: Declaration
) {

  def submitDeclaration(userAnswers: UserAnswers, version: Phase)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    apiConnector.submitDeclaration(declaration.transform(userAnswers, version), version)

  def get(mrn: String, version: Phase)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Messages]] =
    apiConnector.getArrival(mrn, version).flatMap {
      _.traverse {
        arrival => apiConnector.getMessages(arrival.id, version)
      }
    }
}
