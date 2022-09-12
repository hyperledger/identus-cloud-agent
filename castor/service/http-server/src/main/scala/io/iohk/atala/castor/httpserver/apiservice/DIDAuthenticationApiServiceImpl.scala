package io.iohk.atala.castor.httpserver.apiservice

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.*
import io.iohk.atala.castor.core.service.DIDAuthenticationService
import io.iohk.atala.castor.openapi.api.DIDAuthenticationApiService
import io.iohk.atala.castor.openapi.model.{
  AuthenticationChallengeSubmissionRequest,
  AuthenticationChallengeSubmissionResponse,
  CreateAuthenticationChallengeRequest,
  CreateAuthenticationChallengeResponse,
  ErrorResponse
}
import zio.*

// TODO: replace with actual implementation
final class DIDAuthenticationApiServiceImpl(service: DIDAuthenticationService)(using runtime: Runtime[Any])
    extends DIDAuthenticationApiService
    with AkkaZioSupport {

  override def createDidAuthenticationChallenge(
      createAuthenticationChallengeRequest: CreateAuthenticationChallengeRequest
  )(implicit
      toEntityMarshallerCreateAuthenticationChallengeResponse: ToEntityMarshaller[
        CreateAuthenticationChallengeResponse
      ],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val mockResult = CreateAuthenticationChallengeResponse(
      challenge = "eyJhbGciOiJIUzI1NiIsInR5c...0eu8Ri_WSPSsBTlCes2YMpuB1mHU",
      subject = Some("did:example:123456789abcdefghi")
    )
    onZioSuccess(ZIO.succeed(mockResult)) { createDidAuthenticationChallenge200(_) }
  }

  override def createDidAuthenticationChallengeSubmission(
      authenticationChallengeSubmissionRequest: AuthenticationChallengeSubmissionRequest
  )(implicit
      toEntityMarshallerAuthenticationChallengeSubmissionResponse: ToEntityMarshaller[
        AuthenticationChallengeSubmissionResponse
      ],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val mockResult = AuthenticationChallengeSubmissionResponse(
      success = true,
      state = Some("af72a673-7fb5-463d-9966-6a8c6a2cc2e8")
    )
    onZioSuccess(ZIO.succeed(mockResult)) { createDidAuthenticationChallengeSubmission200(_) }
  }

}

object DIDAuthenticationApiServiceImpl {
  val layer: URLayer[DIDAuthenticationService, DIDAuthenticationApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[DIDAuthenticationService]
    } yield DIDAuthenticationApiServiceImpl(svc)(using rt)
  }
}
