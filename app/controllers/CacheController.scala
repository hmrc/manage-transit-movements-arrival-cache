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

import controllers.actions.Actions
import models.{Metadata, SubmissionStatus}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.CacheRepository
import services.DateTimeService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class CacheController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  cacheRepository: CacheRepository,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def get(mrn: String): Action[AnyContent] = actions.authenticate().async {
    implicit request =>
      cacheRepository
        .get(mrn, request.eoriNumber)
        .map {
          case Some(userAnswers) =>
            Ok(Json.toJson(userAnswers))
          case None =>
            logger.warn(s"No document found for MRN '$mrn' and EORI '${request.eoriNumber}'")
            NotFound
        }
        .recover {
          case e =>
            logger.error("Failed to read user answers from mongo", e)
            InternalServerError
        }
  }

  def post(mrn: String): Action[JsValue] = actions.authenticate().async(parse.json) {
    implicit request =>
      request.body.validate[Metadata] match {
        case JsSuccess(data, _) =>
          if (request.eoriNumber == data.eoriNumber) {
            set(data)
          } else {
            logger.warn(s"Enrolment EORI (${request.eoriNumber}) does not match EORI in user answers (${data.eoriNumber})")
            Future.successful(Forbidden)
          }
        case JsError(errors) =>
          logger.warn(s"Failed to validate request body as UserAnswers: $errors")
          Future.successful(BadRequest)
      }
  }

  def put(): Action[JsValue] = actions.authenticate().async(parse.json) {
    implicit request =>
      request.body.validate[String] match {
        case JsSuccess(mrn, _) =>
          set(Metadata(mrn, request.eoriNumber, Json.obj(), SubmissionStatus.NotSubmitted))
        case JsError(errors) =>
          logger.warn(s"Failed to validate request body as String: $errors")
          Future.successful(BadRequest)
      }
  }

  private def set(data: Metadata): Future[Status] =
    cacheRepository
      .set(data)
      .map {
        case true => Ok
        case false =>
          logger.error("Write was not acknowledged")
          InternalServerError
      }
      .recover {
        case e =>
          logger.error("Failed to write user answers to mongo", e)
          InternalServerError
      }

  def delete(mrn: String): Action[AnyContent] = actions.authenticate().async {
    implicit request =>
      cacheRepository
        .remove(mrn, request.eoriNumber)
        .map {
          _ => Ok
        }
        .recover {
          case e =>
            logger.error("Failed to delete draft", e)
            InternalServerError
        }
  }

  def getAll(mrn: Option[String] = None, limit: Option[Int] = None, skip: Option[Int] = None, sortBy: Option[String] = None): Action[AnyContent] =
    actions.authenticate().async {
      implicit request =>
        cacheRepository
          .getAll(request.eoriNumber, mrn, limit, skip, sortBy)
          .map(
            result => Ok(result.toHateoas(dateTimeService.expiresInDays))
          )
          .recover {
            case e =>
              logger.error("Failed to read user answers summary from mongo", e)
              InternalServerError
          }
    }
}
