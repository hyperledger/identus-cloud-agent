package io.iohk.atala.pollux

import io.iohk.atala.agent.server.http.ZHttp4sBlazeServer
import io.iohk.atala.api.http.{BadRequest, NotFound}
import io.iohk.atala.pollux.core.service.CredentialSchemaServiceImpl
import io.iohk.atala.pollux.credentialschema.*
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
import sttp.client3.{DeserializationException, ResponseException, SttpBackend, UriContext, basicRequest, Response as R}
import sttp.model.{StatusCode, Uri}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.interceptor.RequestResult.Response
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.ZIO.*
import zio.interop.catz.*
import zio.interop.catz.implicits.*
import zio.json.*
import zio.json.ast.Json
import zio.json.ast.Json.*
import zio.stream.ZSink
import zio.stream.ZStream.unfold
import zio.test.*
import zio.test.Assertion.*
import zio.test.Gen.*
import zio.test.TestAspect.*

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

object CredentialSchemaBasicSpec extends ZIOSpecDefault with CredentialSchemaTestTools:

  private val schemaInput = CredentialSchemaInput(
    name = "TestSchema",
    version = "1.0.0",
    description = Option("schema description"),
    `type` = "json",
    schema = """{"first_name":  "string", "dob": "datetime"}"""
      .fromJson[zio.json.ast.Json]
      .toOption
      .get,
    tags = List("test")
  )

  def spec = (
    schemaCreateAndGetOperationsSpec
      @@ nondeterministic @@ sequential @@ timed @@ migrate(
        schema = "public",
        paths = "classpath:sql/pollux"
      )
  ).provideSomeLayerShared(testEnvironmentLayer)

  private val schemaCreateAndGetOperationsSpec = {
    val backendZIO = ZIO.service[CredentialSchemaController].map(httpBackend)
    def createSchemaResponseZIO = for {
      backend <- backendZIO
      response <- basicRequest
        .post(credentialSchemaUriBase)
        .body(schemaInput.toJsonPretty)
        .response(asJsonAlways[CredentialSchemaResponse])
        .send(backend)
    } yield response
    def getSchemaZIO(uuid: UUID) = for {
      backend <- backendZIO
      response <- basicRequest
        .get(credentialSchemaUriBase.addPath(uuid.toString))
        .response(asJsonAlways[CredentialSchemaResponse])
        .send(backend)

      fetchedSchema <- fromEither(response.body)
    } yield fetchedSchema

    suite("schema-registry create and get by ID operations logic")(
      test("create the new schema") {
        for {
          response <- createSchemaResponseZIO
          statusCodeIs201 = assert(response.code)(equalTo(StatusCode.Created))

          credentialSchema <- fromEither(response.body)
          actualFields = CredentialSchemaInput(
            name = credentialSchema.name,
            version = credentialSchema.version,
            description = Option(credentialSchema.description),
            `type` = credentialSchema.`type`,
            schema = credentialSchema.schema,
            tags = credentialSchema.tags
          )

          credentialSchemaIsCreated = assert(schemaInput)(equalTo(actualFields))

          fetchedSchema <- getSchemaZIO(credentialSchema.guid)

          credentialSchemaIsFetched = assert(fetchedSchema)(equalTo(credentialSchema))

        } yield statusCodeIs201 && credentialSchemaIsCreated && credentialSchemaIsFetched
      },
      test("get the schema by the wrong id") {
        for {
          backend <- backendZIO
          uuid = UUID.randomUUID()

          response <- basicRequest
            .get(credentialSchemaUriBase.addPath(uuid.toString))
            .response(asJsonAlways[NotFound])
            .send(backend)
        } yield assert(response.code)(equalTo(StatusCode.NotFound))
      }
    )
  }
