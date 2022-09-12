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
  ): Route = onZioSuccess(ZIO.succeed("hello createDidAuthenticationChallenge")) { complete(_) }

  override def createDidAuthenticationChallengeSubmission(
      authenticationChallengeSubmissionRequest: AuthenticationChallengeSubmissionRequest
  )(implicit
      toEntityMarshallerAuthenticationChallengeSubmissionResponse: ToEntityMarshaller[
        AuthenticationChallengeSubmissionResponse
      ],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = onZioSuccess(ZIO.succeed("hello createDidAuthenticationChallengeSubmission")) { complete(_) }

}

object DIDAuthenticationApiServiceImpl {
  val layer: URLayer[DIDAuthenticationService, DIDAuthenticationApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[DIDAuthenticationService]
    } yield DIDAuthenticationApiServiceImpl(svc)(using rt)
  }
}
