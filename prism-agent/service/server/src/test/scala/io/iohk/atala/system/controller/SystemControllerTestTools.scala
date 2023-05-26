package io.iohk.atala.system.controller

import io.iohk.atala.api.http.ErrorResponse
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{DeserializationException, Response, ResponseException, SttpBackend, UriContext, basicRequest}
import sttp.model.{StatusCode, Uri}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.config.{ReadError, read}
import zio.config.typesafe.TypesafeConfigSource
import zio.json.ast.Json.*
import zio.json.{DecoderOps, EncoderOps, JsonDecoder}
import zio.metrics.connectors.prometheus
import zio.stream.ZSink
import zio.stream.ZSink.*
import zio.stream.ZStream.unfold
import zio.test.TestAspect.*
import zio.test.{Gen, Spec, ZIOSpecDefault}
import zio.{Layer, RIO, Task, URLayer, ZIO, ZLayer}
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.{MetricsConfig, prometheus}
import zio.metrics.jvm.DefaultJvmMetrics
import io.iohk.atala.system.controller.http.HealthInfo
import zio.durationInt
import com.typesafe.config.ConfigFactory
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.agent.server.SystemModule.configLayer
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.connect.core.repository.ConnectionRepositoryInMemory
import io.iohk.atala.connect.core.service.ConnectionServiceImpl
import io.iohk.atala.connect.sql.repository.JdbcConnectionRepository
import io.iohk.atala.pollux.core.repository.{CredentialRepositoryInMemory, CredentialSchemaRepository}
import io.iohk.atala.pollux.core.service.{CredentialSchemaServiceImpl, CredentialServiceImpl}
import io.iohk.atala.pollux.credentialschema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.credentialschema.controller.{CredentialSchemaController, CredentialSchemaControllerImpl}
import io.iohk.atala.pollux.credentialschema.http.{
  CredentialSchemaInput,
  CredentialSchemaResponse,
  CredentialSchemaResponsePage
}
import io.iohk.atala.pollux.sql.repository.JdbcCredentialSchemaRepository
import io.iohk.atala.container.util.MigrationAspects.*
import io.iohk.atala.container.util.PostgresLayer.*
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.issue.controller.http.{
  CreateIssueCredentialRecordRequest,
  IssueCredentialRecord,
  IssueCredentialRecordPage
}
import io.iohk.atala.pollux.vc.jwt.*
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{DeserializationException, Response, ResponseException, SttpBackend, UriContext, basicRequest}
import sttp.model.{StatusCode, Uri}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.config.{ReadError, read}
import zio.config.typesafe.TypesafeConfigSource
import zio.json.ast.Json.*
import zio.json.{DecoderOps, EncoderOps, JsonDecoder}
import zio.stream.ZSink
import zio.stream.ZSink.*
import zio.stream.ZStream.unfold
import zio.test.TestAspect.*
import zio.test.{Gen, Spec, ZIOSpecDefault}
import zio.{Layer, RIO, Task, URLayer, ZIO, ZLayer}

import java.time.OffsetDateTime
import java.time.OffsetDateTime

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
    import sttp.tapir.server.interceptor.RequestResult.Response
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
