package org.hyperledger.identus.system.controller

import sttp.client3.testing.SttpBackendStub
import sttp.client3.{DeserializationException, Response, UriContext}
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.metrics.connectors.micrometer
import zio.*
import zio.metrics.jvm.DefaultJvmMetrics
import org.hyperledger.identus.system.controller.http.HealthInfo
import org.hyperledger.identus.agent.server.SystemModule.configLayer
import org.hyperledger.identus.agent.server.config.AppConfig
import org.hyperledger.identus.agent.server.http.CustomServerInterceptors
import sttp.monad.MonadError
import zio.metrics.connectors.micrometer.MicrometerConfig
import zio.test.ZIOSpecDefault
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}

trait SystemControllerTestTools {
  self: ZIOSpecDefault =>

  type HealthInfoResponse =
    Response[Either[DeserializationException[String], HealthInfo]]
  type MetricsResponse =
    Response[
      Either[String, String]
    ]

  private val controllerLayer = ZLayer.succeed(PrometheusMeterRegistry(PrometheusConfig.DEFAULT)) >+>
    configLayer >+>
    ZLayer.succeed(MicrometerConfig.default) >+>
    DefaultJvmMetrics.live.unit >+>
    micrometer.micrometerLayer >+>
    SystemControllerImpl.layer

  val testEnvironmentLayer = zio.test.testEnvironment ++
    controllerLayer

  val systemUriBase = uri"http://test.com/_system/"

  def bootstrapOptions[F[_]](monadError: MonadError[F]): CustomiseInterceptors[F, Any] = {
    new CustomiseInterceptors[F, Any](_ => ())
      .exceptionHandler(CustomServerInterceptors.exceptionHandler)
      .rejectHandler(CustomServerInterceptors.rejectHandler)
      .decodeFailureHandler(CustomServerInterceptors.decodeFailureHandler)
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
