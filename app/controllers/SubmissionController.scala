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

package controllers

import controllers.actions.AuthenticateActionProvider
import models.AuditType.ArrivalNotification
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import repositories.CacheRepository
import services.{ApiService, AuditService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SubmissionController @Inject() (
  cc: ControllerComponents,
  authenticate: AuthenticateActionProvider,
  apiService: ApiService,
  cacheRepository: CacheRepository,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def post(): Action[JsValue] = authenticate().async(parse.json) {
    implicit request =>
      request.body.validate[String] match {
        case JsSuccess(mrn, _) =>
          cacheRepository.get(mrn, request.eoriNumber).flatMap {
            case Some(userAnswers) =>
              apiService.submitDeclaration(userAnswers).map {
                case Right(response) =>
                  auditService.audit(ArrivalNotification, userAnswers)
                  Ok(response.body)
                case Left(error) =>
                  error
              }
            case None => Future.successful(InternalServerError)
          }
        case JsError(errors) =>
          logger.warn(s"Failed to validate request body as String: $errors")
          Future.successful(BadRequest)
      }
  }
}
