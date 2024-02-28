package io.iohk.atala.iam.oidc

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.iam.authentication.{Authenticator, Authorizer, DefaultAuthenticator, SecurityLogic}
import io.iohk.atala.iam.oidc.http.{CredentialErrorResponse, CredentialRequest, ImmediateCredentialResponse}
import sttp.tapir.ztapir.*
import zio.*

class CredentialIssuerServerEndpoints(
    authenticator: Authenticator[BaseEntity]
) {

  val credentialServersEndpoint: ZServerEndpoint[Any, Any] =
    CredentialIssuerEndpoints.credentialEndpoint
      .zServerSecurityLogic(
        SecurityLogic // TODO: add OIDC client authenticator
          .authenticate(_)(authenticator)
          .mapError(Left[ErrorResponse, CredentialErrorResponse])
      )
      .serverLogic { wac =>
        { case (ctx: RequestContext, didRef: String, request: CredentialRequest) =>
          ZIO.succeed(ImmediateCredentialResponse("credential"))
        }
      }

  val all: List[ZServerEndpoint[Any, Any]] = List(credentialServersEndpoint)
}

object CredentialIssuerServerEndpoints {
  def all: URIO[DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
      oidcEndpoints = new CredentialIssuerServerEndpoints(authenticator)
    } yield oidcEndpoints.all
  }
}
