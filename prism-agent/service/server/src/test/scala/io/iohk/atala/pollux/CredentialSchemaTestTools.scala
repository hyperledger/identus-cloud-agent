package io.iohk.atala.pollux

import io.iohk.atala.api.http.BadRequest
import io.iohk.atala.pollux.core.repository.CredentialSchemaRepository
import io.iohk.atala.pollux.core.service.CredentialSchemaServiceImpl
import io.iohk.atala.pollux.credentialschema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.credentialschema.controller.{CredentialSchemaController, CredentialSchemaControllerImpl}
import io.iohk.atala.pollux.credentialschema.http.{
  CredentialSchemaInput,
  CredentialSchemaResponsePage,
  CredentialSchemaResponse
}
import io.iohk.atala.pollux.sql.repository.JdbcCredentialSchemaRepository
import io.iohk.atala.pollux.test.container.MigrationAspects.*
import io.iohk.atala.pollux.test.container.PostgresTestContainer.*
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{DeserializationException, Response, ResponseException, SttpBackend, UriContext, basicRequest}
import sttp.model.{StatusCode, Uri}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.json.ast.Json.*
import zio.json.{DecoderOps, EncoderOps, JsonDecoder}
import zio.stream.ZSink
import zio.stream.ZSink.*
import zio.stream.ZStream.unfold
import zio.test.TestAspect.*
import zio.test.{Gen, Spec, ZIOSpecDefault}
import zio.{RIO, Task, URLayer, ZIO, ZLayer}

import java.time.OffsetDateTime

trait CredentialSchemaTestTools {
  self: ZIOSpecDefault =>

  type SchemaBadRequestResponse =
    Response[Either[DeserializationException[String], BadRequest]]
  type SchemaResponse =
    Response[Either[DeserializationException[String], CredentialSchemaResponse]]
  type SchemaPageResponse =
    Response[
      Either[DeserializationException[String], CredentialSchemaResponsePage]
    ]

  private val pgLayer = postgresLayer(verbose = false)
  private val transactorLayer = pgLayer >>> hikariConfigLayer >>> transactor
  private val controllerLayer = transactorLayer >>>
    JdbcCredentialSchemaRepository.layer >+>
    CredentialSchemaServiceImpl.layer >+>
    CredentialSchemaControllerImpl.layer

  val testEnvironmentLayer = zio.test.testEnvironment ++
    pgLayer ++
    transactorLayer ++
    controllerLayer

  val credentialSchemaUriBase = uri"http://test.com/schema-registry/schemas"

  def bootstrapOptions[F[_]](monadError: MonadError[F]) = {
    import sttp.tapir.server.interceptor.RequestResult.Response
    new CustomiseInterceptors[F, Any](_ => ())
      .defaultHandlers(BadRequest.failureResponseHandler)
  }

  def httpBackend(controller: CredentialSchemaController) = {
    val schemaRegistryEndpoints = SchemaRegistryServerEndpoints(controller)

    val backend =
      TapirStubInterpreter(
        bootstrapOptions(new RIOMonadError[Any]),
        SttpBackendStub(new RIOMonadError[Any])
      )
        .whenServerEndpoint(schemaRegistryEndpoints.createSchemaServerEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(schemaRegistryEndpoints.getSchemaByIdServerEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(
          schemaRegistryEndpoints.lookupSchemasByQueryServerEndpoint
        )
        .thenRunLogic()
        .backend()
    backend
  }

  def deleteAllCredentialSchemas: RIO[CredentialSchemaRepository[Task], Long] = {
    for {
      repository <- ZIO.service[CredentialSchemaRepository[Task]]
      count <- repository.deleteAll()
    } yield count
  }
}

trait CredentialSchemaGen {
  self: ZIOSpecDefault with CredentialSchemaTestTools =>
  object Generator {
    val schemaName = Gen.alphaNumericStringBounded(4, 12)
    val schemaVersion = Gen.int(1, 5).map(i => s"$i.0")
    val schemaDescription = Gen.alphaNumericStringBounded(5, 30)
    val schemaAttribute = Gen.alphaNumericStringBounded(3, 9)
    val schemaAttributes = Gen.setOfBounded(1, 4)(schemaAttribute).map(_.toList)
    val schemaAuthored = Gen.offsetDateTime(
      min = OffsetDateTime.now().minusMonths(6),
      max = OffsetDateTime.now()
    )
    val schemaTag: Gen[Any, String] = Gen.alphaNumericStringBounded(3, 5)
    val schemaTags: Gen[Any, List[String]] =
      Gen.setOfBounded(0, 3)(schemaTag).map(_.toList)

    val schemaInput = for {
      name <- schemaName
      version <- schemaVersion
      description <- schemaDescription
      attributes <- schemaAttributes
      tags <- schemaTags
    } yield CredentialSchemaInput(
      name = name,
      version = version,
      description = Some(description),
      `type` = "json",
      schema = Arr(Obj("first_name" -> Str("String"))),
      tags = tags
    )
  }

  def generateSchemasN(
      count: Int
  ): ZIO[CredentialSchemaController, Throwable, List[CredentialSchemaInput]] =
    for {
      controller <- ZIO.service[CredentialSchemaController]
      backend = httpBackend(controller)
      inputs <- Generator.schemaInput.runCollectN(count)
      _ <- inputs
        .map(in =>
          basicRequest
            .post(credentialSchemaUriBase)
            .body(in.toJsonPretty)
            .response(asJsonAlways[CredentialSchemaResponse])
            .send(backend)
        )
        .reduce((l, r) => l.flatMap(_ => r))
    } yield inputs
}
