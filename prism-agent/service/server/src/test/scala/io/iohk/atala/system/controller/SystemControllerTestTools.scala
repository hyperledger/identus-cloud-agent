package io.iohk.atala.system.controller

import io.iohk.atala.api.http.ErrorResponse
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{DeserializationException, Response, UriContext}
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.metrics.connectors.prometheus
import zio.*
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.MetricsConfig
import zio.metrics.jvm.DefaultJvmMetrics
import io.iohk.atala.system.controller.http.HealthInfo
import zio.durationInt
import io.iohk.atala.agent.server.SystemModule.configLayer
import io.iohk.atala.agent.server.config.AppConfig
import sttp.monad.MonadError
import zio.test.ZIOSpecDefault

trait SystemControllerTestTools {
  self: ZIOSpecDefault =>

  type HealthInfoResponse =
    Response[Either[DeserializationException[String], HealthInfo]]
  type MetricsResponse =
    Response[
      Either[String, String]
    ]

  private val controllerLayer = prometheus.publisherLayer >+>
    configLayer >+>
    ZLayer.succeed(MetricsConfig(1.seconds)) >+>
    DefaultJvmMetrics.live.unit >+>
    prometheus.prometheusLayer >+>
    SystemControllerImpl.layer

  val testEnvironmentLayer = zio.test.testEnvironment ++
    controllerLayer

  val systemUriBase = uri"http://test.com/_system/"

  def bootstrapOptions[F[_]](monadError: MonadError[F]): CustomiseInterceptors[F, Any] = {
    new CustomiseInterceptors[F, Any](_ => ())
      .defaultHandlers(ErrorResponse.failureResponseHandler)
  }

  def httpBackend(controller: SystemController) = {
    val systemEndpoints = SystemServerEndpoints(controller)

    val backend =
      TapirStubInterpreter(
        bootstrapOptions(new RIOMonadError[Any]),
        SttpBackendStub(new RIOMonadError[Any])
      )
        .whenServerEndpoint(systemEndpoints.healthEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(systemEndpoints.metricsEndpoint)
        .thenRunLogic()
        .backend()
    backend
  }

}
