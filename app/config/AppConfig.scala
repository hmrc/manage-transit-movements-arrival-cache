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

package config

import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class AppConfig @Inject() (config: Configuration) {

  val appName: String = config.get[String]("appName")

  val mongoTtlInDays: Int  = config.get[Int]("mongodb.ttlInDays")
  val lockTTLInMins: Int   = config.get[Int]("mongodb.lockTTLInMins")
  val maxRowsReturned: Int = config.get[Int]("mongodb.maxRowsReturned")

  val enrolmentKey: String        = config.get[String]("enrolment.key")
  val enrolmentIdentifier: String = config.get[String]("enrolment.identifier")

  val apiUrl: String = config.get[Service]("microservice.services.common-transit-convention-traders").baseUrl

  val encryptionKey: String        = config.get[String]("encryption.key")
  val encryptionEnabled: Boolean   = config.get[Boolean]("encryption.enabled")
  lazy val replaceIndexes: Boolean = config.get[Boolean]("feature-flags.replace-indexes")
}
