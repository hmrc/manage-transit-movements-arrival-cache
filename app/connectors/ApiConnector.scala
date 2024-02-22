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
import play.api.Logging
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class ApiConnector @Inject() (http: HttpClientV2, appConfig: AppConfig)(implicit ec: ExecutionContext) extends HttpErrorFunctions with Logging {

  def submitDeclaration(xml: NodeSeq)(implicit hc: HeaderCarrier): Future[Either[Result, HttpResponse]] = {
    val url = url"${appConfig.apiUrl}/movements/arrivals"
    http
      .post(url)
      .setHeader(ACCEPT -> "application/vnd.hmrc.2.0+json")
      .setHeader(CONTENT_TYPE -> "application/xml")
      .withBody(xml)
      .execute[HttpResponse]
      .map {
        response =>
          response.status match {
            case x if is2xx(x) =>
              Right(response)
            case BAD_REQUEST =>
              logger.info(s"ApiConnector:submitDeclaration: bad request")
              Left(BadRequest("ApiConnector:submitDeclaration: bad request"))
            case e =>
              logger.error(s"ApiConnector:submitDeclaration: something went wrong: $e")
              Left(InternalServerError("ApiConnector:submitDeclaration: something went wrong"))
          }
      }
  }
}
