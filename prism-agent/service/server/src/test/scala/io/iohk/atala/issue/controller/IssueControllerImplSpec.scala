package io.iohk.atala.issue.controller

import io.iohk.atala.agent.server.http.ZHttp4sBlazeServer
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.container.util.MigrationAspects.migrate
import io.iohk.atala.issue.controller.http.AcceptCredentialOfferRequest
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

object IssueControllerImplSpec extends ZIOSpecDefault with IssueControllerTestTools {

  def spec = (httpErrorResponses @@ migrate(
    schema = "public",
    paths = "classpath:sql/pollux"
  )).provideSomeLayerShared(testEnvironmentLayer)

  private val httpErrorResponses = suite("IssueControllerImp http failure cases")(
    test("provide incorrect subjectId to endpoint") {
      for {
        issueControllerService <- ZIO.service[IssueController]
        backend = httpBackend(issueControllerService)
        response: IssueCredentialBadRequestResponse <- basicRequest
          .post(uri"${issueUriBase}/records/12345/accept-offer")
          .body(AcceptCredentialOfferRequest("subjectId").toJsonPretty)
          .response(asJsonAlways[ErrorResponse])
          .send(backend)

        isItABadRequestStatusCode = assert(response.code)(equalTo(StatusCode.BadRequest))
        theBodyWasParsedFromJsonAsABadRequest = assert(response.body)(
          isRight(
            isSubtype[ErrorResponse](
              equalTo(
                ErrorResponse.badRequest(
                  "Unsupported DID format",
                  Some(s"The following DID is not supported: subjectId")
                )
              )
            )
          )
        )
      } yield isItABadRequestStatusCode && theBodyWasParsedFromJsonAsABadRequest
    }
  )

}
