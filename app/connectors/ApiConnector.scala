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

import api.submission.Header.scope
import api.submission._
import config.AppConfig
import generated.{CC007CType, MESSAGE_FROM_TRADERSequence, MessageType007, PhaseIDtype}
import models.UserAnswers
import play.api.Logging
import play.api.http.HeaderNames
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError}
import scalaxb.DataRecord
import scalaxb.`package`.toXML
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, HttpErrorFunctions, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ApiConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) extends HttpErrorFunctions with Logging {

  private val requestHeaders = Seq(
    HeaderNames.ACCEPT       -> "application/vnd.hmrc.2.0+json",
    HeaderNames.CONTENT_TYPE -> "application/xml"
  )

  def createPayload(userAnswers: UserAnswers): CC007CType = {
    val message: MESSAGE_FROM_TRADERSequence = Header.message(userAnswers)
    val messageType: MessageType007          = Header.messageType
    val correlationIdentifier                = Header.correlationIdentifier
    val transitOperation                     = TransitOperation.transform(userAnswers)
    val authorisations                       = Authorisations.transform(userAnswers)
    val customsOfficeOfDestination           = DestinationDetails.customsOfficeOfDestination(userAnswers)
    val traderAtDestination                  = DestinationDetails.traderAtDestination(userAnswers)
    val consignment                          = Consignment.transform(userAnswers)

    CC007CType(
      message,
      messageType,
      correlationIdentifier,
      transitOperation,
      authorisations,
      customsOfficeOfDestination,
      traderAtDestination,
      consignment,
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
          logger.debug(s"ApiConnector:submitDeclaration: success: ${response.status}-${response.body}")
          Right(response)
      }
      .recover {
        case httpEx: BadRequestException =>
          logger.warn(s"ApiConnector:submitDeclaration: bad request: ${httpEx.responseCode}-${httpEx.getMessage}")
          Left(BadRequest("ApiConnector:submitDeclaration: bad request"))
        case e: Exception =>
          logger.error(s"ApiConnector:submitDeclaration: something went wrong: ${e.getMessage}")
          Left(InternalServerError("ApiConnector:submitDeclaration: something went wrong"))
      }
  }
}
