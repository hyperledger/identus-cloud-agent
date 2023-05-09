package io.iohk.atala.pollux

import io.iohk.atala.agent.server.http.ZHttp4sBlazeServer
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.pollux.credentialschema.*
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaController
import io.iohk.atala.pollux.credentialschema.http.{
  CredentialSchemaInput,
  CredentialSchemaResponse,
  CredentialSchemaResponsePage
}
import io.iohk.atala.container.util.MigrationAspects.migrate
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{DeserializationException, Response, ResponseException, SttpBackend, UriContext, basicRequest}
import sttp.model.{StatusCode, Uri}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.interceptor.RequestResult.Response
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.ast.Json.*
import zio.json.{DecoderOps, EncoderOps, JsonDecoder}
import zio.stream.ZSink
import zio.stream.ZSink.*
import zio.stream.ZStream.unfold
import zio.test.*
import zio.test.Assertion.*
import zio.test.Gen.*
import zio.test.TestAspect.{nondeterministic, sequential}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

object CredentialSchemaFailureSpec extends ZIOSpecDefault with CredentialSchemaTestTools:
  def spec = (schemaBadRequestAsJsonSpec @@ migrate(
    schema = "public",
    paths = "classpath:sql/pollux"
  )).provideSomeLayerShared(testEnvironmentLayer)

  private val schemaBadRequestAsJsonSpec = suite("schema-registry BadRequest as json logic")(
    test("create the schema with wrong json body returns BadRequest as json") {
      for {
        schemaRegistryService <- ZIO.service[CredentialSchemaController]
        backend = httpBackend(schemaRegistryService)
        response: SchemaBadRequestResponse <- basicRequest
          .post(credentialSchemaUriBase)
          .body("""{"foo":"bar"}""")
          .response(asJsonAlways[ErrorResponse])
          .send(backend)

        itIsABadRequestStatusCode = assert(response.code)(equalTo(StatusCode.BadRequest))
        theBodyWasParsedFromJsonAsBadRequest = assert(response.body)(
          isRight(isSubtype[ErrorResponse](Assertion.anything))
        )
      } yield itIsABadRequestStatusCode && theBodyWasParsedFromJsonAsBadRequest
    }
  )
