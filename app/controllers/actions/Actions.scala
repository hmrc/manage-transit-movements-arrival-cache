/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.actions

import models.request.{AuthenticatedRequest, VersionedRequest}
import play.api.mvc.{ActionBuilder, AnyContent, DefaultActionBuilder}

import javax.inject.Inject

class Actions @Inject() (
  buildDefault: DefaultActionBuilder,
  authenticateActionProvider: AuthenticateActionProvider,
  lockActionProvider: LockActionProvider,
  versionedAction: VersionedAction
) {

  def authenticate(): ActionBuilder[AuthenticatedRequest, AnyContent] =
    buildDefault andThen authenticateActionProvider()

  def authenticateAndLock(mrn: String): ActionBuilder[AuthenticatedRequest, AnyContent] =
    authenticate() andThen lockActionProvider(mrn)

  def authenticateAndGetVersion(): ActionBuilder[VersionedRequest, AnyContent] =
    authenticate() andThen versionedAction

  def authenticateAndLockAndGetVersion(mrn: String): ActionBuilder[VersionedRequest, AnyContent] =
    authenticate() andThen lockActionProvider(mrn) andThen versionedAction
}
