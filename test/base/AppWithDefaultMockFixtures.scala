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

package base

import config.AppConfig
import controllers.actions.{AuthenticateActionProvider, FakeAuthenticateActionProvider}
import org.scalatest.{BeforeAndAfterEach, TestSuite}
import models.SensitiveFormats
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContent
import play.api.test.FakeRequest

import java.time.Clock

trait AppWithDefaultMockFixtures extends BeforeAndAfterEach with GuiceOneAppPerSuite {
  self: TestSuite & SpecBase =>

  def fakeRequest: FakeRequest[AnyContent] = FakeRequest("", "")

  lazy val appConfig: AppConfig                        = app.injector.instanceOf[AppConfig]
  implicit lazy val clock: Clock                       = app.injector.instanceOf[Clock]
  implicit lazy val sensitiveFormats: SensitiveFormats = app.injector.instanceOf[SensitiveFormats]

  override def fakeApplication(): Application =
    guiceApplicationBuilder()
      .build()

  // Override to provide custom binding
  def guiceApplicationBuilder(): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure("metrics.enabled" -> false)
      .overrides(
        bind[AuthenticateActionProvider].toInstance(new FakeAuthenticateActionProvider(eoriNumber))
      )
}
