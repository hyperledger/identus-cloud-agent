package io.iohk.atala.pollux.api.spec

import io.iohk.atala.pollux.models.{NotFoundResponse, VerifiableCredentialsSchema, VerifiableCredentialsSchemaInput}
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{UriContext, basicRequest}
import sttp.tapir.server.stub.TapirStubInterpreter
import zio.test.Assertion.*
import zio.test.{ZIOSpecDefault, assertZIO}
import sttp.client3.ziojson.*
import sttp.tapir.ztapir.RIOMonadError
import io.iohk.atala.pollux.stub.SchemaEndpointsInMemory.{createSchemaServerEndpoint, getSchemaByIdServerEndpoint}
import VerifiableCredentialsSchemaInput.encoder
import io.iohk.atala.pollux.services.SchemaServiceInMemory
import sttp.model.StatusCode
import zio.json.{DecoderOps, EncoderOps}

import java.time.ZonedDateTime
import java.util.UUID

// Here the server endpoints are tested without running the server and client.
object SchemaEndpointsSpec extends ZIOSpecDefault:

  val schemaId = UUID.randomUUID()

  val schemaInput = VerifiableCredentialsSchemaInput(
    Option(schemaId),
    name = "test schema",
    version = "1.0",
    description = Option("schema description"),
    attributes = List("first_name", "dob"),
    authored = Option(ZonedDateTime.now()),
    tags = List("test")
  )

  val schema = VerifiableCredentialsSchema(schemaInput)

  def spec = suite("Schema Endpoints spec")(
    test("create new schema") {
      // given
      val backendStub = TapirStubInterpreter(SttpBackendStub(new RIOMonadError[Any]))
        .whenServerEndpoint(createSchemaServerEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(getSchemaByIdServerEndpoint)
        .thenRunLogic()
        .backend()

      // when
      val response = basicRequest
        .post(uri"http://test.com/schema-registry/schemas")
        .body(schemaInput.toJsonPretty)
        .response(asJson[VerifiableCredentialsSchema])
        .send(backendStub)

      // then
      assertZIO(response.map(_.body))(isRight(equalTo(schema)))
    },
    test("get schema by id") {
      // given
      val backendStub = TapirStubInterpreter(SttpBackendStub(new RIOMonadError[Any]))
        .whenServerEndpoint(getSchemaByIdServerEndpoint)
        .thenRunLogic()
        .backend()

      SchemaServiceInMemory.instance.storage.put(schemaId, schema)

      // when
      val response2 = basicRequest
        .get(uri"http://test.com/schema-registry/schemas/$schemaId")
        .response(asJson[VerifiableCredentialsSchema])
        .send(backendStub)

      // then
      assertZIO(response2.map(_.body))(isRight(equalTo(schema)))
    },
    test("get schema by id - not found") {
      // given
      val backendStub = TapirStubInterpreter(SttpBackendStub(new RIOMonadError[Any]))
        .whenServerEndpoint(getSchemaByIdServerEndpoint)
        .thenRunLogic()
        .backend()

      // when
      val uuid = UUID.randomUUID()
      val response = basicRequest
        .get(uri"http://test.com/schema-registry/schemas/$uuid")
        .response(asJson[NotFoundResponse])
        .send(backendStub)

      // then

      assertZIO(response.map(_.code))(equalTo(StatusCode.NotFound))
    }
  )
