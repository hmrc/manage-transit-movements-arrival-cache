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

package services

import base.{AppWithDefaultMockFixtures, SpecBase}
import com.codahale.metrics.{Counter, MetricRegistry}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

class MetricsServiceSpec extends SpecBase with AppWithDefaultMockFixtures with ScalaCheckPropertyChecks {

  private val mockMetrics = mock[Metrics]

  private val mockMetricRegistry = mock[MetricRegistry]

  override def guiceApplicationBuilder(): GuiceApplicationBuilder =
    super
      .guiceApplicationBuilder()
      .overrides(bind[Metrics].toInstance(mockMetrics))

  private val service = app.injector.instanceOf[MetricsService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetrics)
    reset(mockMetricRegistry)
    when(mockMetrics.defaultRegistry).thenReturn(mockMetricRegistry)
  }

  "MetricsService" when {
    "increment" when {
      "2xx response" in {
        when(mockMetricRegistry.counter(any())).thenReturn(new Counter())

        val response = HttpResponse(OK, "")

        service.increment(response)

        verify(mockMetricRegistry).counter(eqTo("ArrivalNotification"))
      }

      "4xx response" in {
        forAll(Gen.choose(400, 499)) {
          status =>
            beforeEach()

            when(mockMetricRegistry.counter(any())).thenReturn(new Counter())

            val response = HttpResponse(status, "")

            service.increment(response)

            verify(mockMetricRegistry).counter(eqTo("ArrivalNotification-4xx"))
        }
      }

      "5xx response" in {
        forAll(Gen.choose(500, 599)) {
          status =>
            beforeEach()

            when(mockMetricRegistry.counter(any())).thenReturn(new Counter())

            val response = HttpResponse(status, "")

            service.increment(response)

            verify(mockMetricRegistry).counter(eqTo("ArrivalNotification-5xx"))
        }
      }
    }
  }
}
