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
import models.{Messages, SubmissionStatus}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.CacheRepository
import services.{ApiService, AuditService, MetricsService}
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SubmissionController @Inject() (
  cc: ControllerComponents,
  authenticate: AuthenticateActionProvider,
  apiService: ApiService,
  cacheRepository: CacheRepository,
  auditService: AuditService,
  metricsService: MetricsService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def post(): Action[JsValue] = authenticate().async(parse.json) {
    implicit request =>
      val eori                                        = request.eoriNumber
      def log(message: String, args: String*): String = s"SubmissionController:post:${args.mkString(":")} - $message"

      request.body.validate[String] match {
        case JsSuccess(mrn, _) =>
          cacheRepository.get(mrn, request.eoriNumber).flatMap {
            case Some(userAnswers) =>
              apiService.submitDeclaration(userAnswers).flatMap {
                response =>
                  metricsService.increment(response.status)
                  response.status match {
                    case status if is2xx(status) =>
                      cacheRepository.set(userAnswers.metadata.copy(submissionStatus = SubmissionStatus.Submitted)).map {
                        _ =>
                          auditService.audit(ArrivalNotification, userAnswers)
                          Ok(response.body)
                      }
                    case BAD_REQUEST =>
                      logger.warn(log("Bad request", eori, mrn))
                      Future.successful(BadRequest)
                    case e =>
                      logger.error(log(s"Something went wrong: $e", eori, mrn))
                      Future.successful(InternalServerError)
                  }
              }
            case None =>
              metricsService.increment(NOT_FOUND)
              logger.error(log("Could not find user answers", eori, mrn))
              Future.successful(NotFound)
          }
        case JsError(errors) =>
          metricsService.increment(BAD_REQUEST)
          logger.warn(log(s"Failed to validate request body as String: $errors", eori))
          Future.successful(BadRequest)
      }
  }

  def get(mrn: String): Action[AnyContent] = authenticate().async {
    implicit request =>
      apiService.get(mrn).map {
        case Some(Messages(Nil)) =>
          logger.info(s"No messages found for MRN $mrn")
          NoContent
        case Some(messages) =>
          Ok(Json.toJson(messages))
        case None =>
          logger.warn(s"No arrival found for MRN $mrn")
          NotFound
      }
  }
}
