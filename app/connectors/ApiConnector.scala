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

package connectors

import config.AppConfig
import models.{Arrival, Arrivals, Messages, Phase}
import play.api.Logging
import play.api.http.HeaderNames.*
import play.api.libs.ws.XMLBodyWritables.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class ApiConnector @Inject() (http: HttpClientV2, appConfig: AppConfig)(implicit ec: ExecutionContext) extends HttpErrorFunctions with Logging {

  def submitDeclaration(xml: NodeSeq, version: Phase)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = url"${appConfig.apiUrl}/movements/arrivals"
    http
      .post(url)
      .setHeader(ACCEPT -> s"application/vnd.hmrc.${version.apiVersion}+json")
      .setHeader(CONTENT_TYPE -> "application/xml")
      .withBody(xml)
      .execute[HttpResponse]
  }

  def getArrival(mrn: String, version: Phase)(implicit hc: HeaderCarrier): Future[Option[Arrival]] = {
    val url = url"${appConfig.apiUrl}/movements/arrivals"
    http
      .get(url)
      .transform(_.withQueryStringParameters("movementReferenceNumber" -> mrn))
      .setHeader(ACCEPT -> s"application/vnd.hmrc.${version.apiVersion}+json")
      .execute[Arrivals]
      .map(_.arrivals.headOption)
  }

  def getMessages(arrivalId: String, version: Phase)(implicit hc: HeaderCarrier): Future[Messages] = {
    val url = url"${appConfig.apiUrl}/movements/arrivals/$arrivalId/messages"
    http
      .get(url)
      .setHeader(ACCEPT -> s"application/vnd.hmrc.${version.apiVersion}+json")
      .execute[Messages]
  }
}
