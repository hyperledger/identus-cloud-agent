package io.iohk.atala.issue.controller

import io.iohk.atala.agent.walletapi.service.{ManagedDIDService, MockManagedDIDService}
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.castor.core.service.MockDIDService
import io.iohk.atala.container.util.MigrationAspects.migrate
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.issue.controller.http.AcceptCredentialOfferRequest
import sttp.client3.ziojson.*
import sttp.client3.{DeserializationException, UriContext, basicRequest}
import sttp.model.StatusCode
import zio.*
import zio.json.EncoderOps
import zio.test.*
import zio.test.Assertion.*

object IssueControllerImplSpec extends ZIOSpecDefault with IssueControllerTestTools {

  def spec = (httpErrorResponses @@ migrate(
    schema = "public",
    paths = "classpath:sql/pollux"
  )).provideSomeLayerShared(MockDIDService.empty ++ MockManagedDIDService.empty >>> testEnvironmentLayer)

  private val httpErrorResponses = suite("IssueControllerImp http failure cases")(
    test("provide incorrect subjectId to endpoint") {
      for {
        issueControllerService <- ZIO.service[IssueController]
        authenticator <- ZIO.service[Authenticator]
        backend = httpBackend(issueControllerService, authenticator)
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
                  "BadRequest",
                  Some(s"Error parsing string as PrismDID: DID syntax must start with 'did:' prefix")
                )
              )
            )
          )
        )
      } yield isItABadRequestStatusCode && theBodyWasParsedFromJsonAsABadRequest
    }
  )

}
