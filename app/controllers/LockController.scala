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
import play.api.Logging
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.LockRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class LockController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  lockRepository: LockRepository
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def checkLock(mrn: String): Action[AnyContent] = actions.authenticate().async {
    implicit request =>
      hc.sessionId
        .map {
          sessionId =>
            lockRepository.lock(sessionId.value, request.eoriNumber, mrn).map {
              case true  => Ok
              case false => Locked
            }
        }
        .getOrElse(Future.successful(BadRequest))
  }

  def deleteLock(mrn: String): Action[AnyContent] = actions.authenticate().async {
    implicit request =>
      hc.sessionId
        .map {
          sessionId =>
            lockRepository.unlock(request.eoriNumber, mrn, sessionId.value).map {
              case true  => Ok
              case false => InternalServerError
            }
        }
        .getOrElse(Future.successful(BadRequest))
  }
}
