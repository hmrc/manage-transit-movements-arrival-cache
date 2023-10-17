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

import api.submission._
import config.AppConfig
import generated.{CC007CType, MESSAGESequence, PhaseIDtype}
import models.UserAnswers
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError}
import scalaxb.DataRecord
import scalaxb.`package`.toXML
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NamespaceBinding

class ApiConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) extends HttpErrorFunctions with Logging {

  private val requestHeaders = Seq(
    HeaderNames.ACCEPT       -> "application/vnd.hmrc.2.0+json",
    HeaderNames.CONTENT_TYPE -> "application/xml"
  )

  private val scope: NamespaceBinding = scalaxb.toScope(Some("ncts") -> "http://ncts.dgtaxud.ec")

  private def createPayload(userAnswers: UserAnswers): CC007CType = {
    val message: MESSAGESequence   = Header.message(userAnswers)
    val transitOperation           = TransitOperation.transform(userAnswers)
    val authorisations             = Authorisations.transform(userAnswers)
    val customsOfficeOfDestination = DestinationDetails.customsOfficeOfDestination(userAnswers)
    val traderAtDestination        = DestinationDetails.traderAtDestination(userAnswers)
    val consignment                = Consignment.transform(userAnswers)

    CC007CType(
      messageSequence1 = message,
      TransitOperation = transitOperation,
      Authorisation = authorisations,
      CustomsOfficeOfDestinationActual = customsOfficeOfDestination,
      TraderAtDestination = traderAtDestination,
      Consignment = consignment,
      attributes = Map("@PhaseID" -> DataRecord(PhaseIDtype.fromString("NCTS5.0", scope)))
    )
  }

  def submitDeclaration(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Either[Result, HttpResponse]] = {

    val declarationUrl  = s"${appConfig.apiUrl}/movements/arrivals"
    val payload: String = toXML[CC007CType](createPayload(userAnswers), "ncts:CC007C", scope).toString

    httpClient
      .POSTString[HttpResponse](declarationUrl, payload, requestHeaders)
      .map {
        response =>
          response.status match {
            case x if is2xx(x) =>
              logger.debug(s"ApiConnector:submitDeclaration: success: ${response.status}-${response.body}")
              Right(response)
            case BAD_REQUEST =>
              logger.info(s"ApiConnector:submitDeclaration: bad request: ${response.body}")
              Left(BadRequest("ApiConnector:submitDeclaration: bad request"))
            case _ =>
              logger.error(s"ApiConnector:submitDeclaration: something went wrong: ${response.body}")
              Left(InternalServerError("ApiConnector:submitDeclaration: something went wrong"))
          }
      }
  }
}
