package io.iohk.atala.pollux.schema

import io.iohk.atala.api.http.NotFoundResponse
import io.iohk.atala.pollux.service.SchemaRegistryService
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{ResponseException, UriContext, basicRequest}
import sttp.tapir.server.interceptor.RequestResult.Response
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.ZIO
import zio.test.Assertion.*
import zio.test.{Assertion, ZIOSpecDefault, assertZIO}
import io.iohk.atala.pollux.schema.*
import io.iohk.atala.pollux.schema.model.VerifiableCredentialSchema
import io.iohk.atala.pollux.service.SchemaRegistryServiceInMemory
import sttp.model.StatusCode
import zio.ZLayer
import zio.json.{DecoderOps, EncoderOps}

import java.time.ZonedDateTime
import java.util.UUID

object SchemaRegistryEndpointsSpec extends ZIOSpecDefault:

  private val schemaId = UUID.randomUUID()

  private val schemaInput = VerifiableCredentialSchema.Input(
    Option(schemaId),
    name = "test schema",
    version = "1.0",
    description = Option("schema description"),
    attributes = List("first_name", "dob"),
    authored = Option(ZonedDateTime.now()),
    tags = List("test")
  )

  private val schema = VerifiableCredentialSchema(schemaInput)

  def httpBackend(schemaRegistryService: SchemaRegistryService) = {
    val schemaRegistryEndpoints = SchemaRegistryServerEndpoints(schemaRegistryService)
    val backend = TapirStubInterpreter(SttpBackendStub(new RIOMonadError[Any]))
      .whenServerEndpoint(schemaRegistryEndpoints.createSchemaServerEndpoint)
      .thenRunLogic()
      .whenServerEndpoint(schemaRegistryEndpoints.getSchemaByIdServerEndpoint)
      .thenRunLogic()
      .backend()
    backend
  }

  def spec = suite("Schema Endpoints spec")(
    createSchemaSpec // , updateSchemaSpec
  ).provideLayer(SchemaRegistryServiceInMemory.layer)

  private val createSchemaSpec = suite("create schema")(
    test("create new schema") {
      for {
        schemaRegistryService <- ZIO.service[SchemaRegistryService]
        backend = httpBackend(schemaRegistryService)

        response = basicRequest
          .post(uri"http://test.com/schema-registry/schemas")
          .body(schemaInput.toJsonPretty)
          .response(asJson[VerifiableCredentialSchema])
          .send(backend)

        assertion <- assertZIO(response.map(_.body))(isRight(equalTo(schema)))
      } yield assertion
    },
    test("create and get a schema by id") {
      for {
        schemaRegistryService <- ZIO.service[SchemaRegistryService]
        backend = httpBackend(schemaRegistryService)

        _ <- basicRequest
          .post(uri"http://test.com/schema-registry/schemas")
          .body(schemaInput.toJsonPretty)
          .send(backend)

        response = basicRequest
          .get(uri"http://test.com/schema-registry/schemas/$schemaId")
          .response(asJson[VerifiableCredentialSchema])
          .send(backend)

        assertion <- assertZIO(response.map(_.body))(isRight(equalTo(schema)))
      } yield assertion
    },
    test("create and get a schema by random id") {
      for {
        schemaRegistryService <- ZIO.service[SchemaRegistryService]
        backend = httpBackend(schemaRegistryService)

        _ <- basicRequest
          .post(uri"http://test.com/schema-registry/schemas")
          .body(schemaInput.toJsonPretty)
          .send(backend)

        uuid = UUID.randomUUID()

        response = basicRequest
          .get(uri"http://test.com/schema-registry/schemas/$uuid")
          .response(asJson[NotFoundResponse])
          .send(backend)

        assertion <- assertZIO(response.map(_.code))(equalTo(StatusCode.NotFound))
      } yield assertion
    }
  )
