package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import zio.*

object SecurityLogic {

  /** Authenticate from a list of credentials one by one until the Entity is found */
  def securityLogic(credentials: Credentials, others: Credentials*)(
      authenticator: Authenticator
  ): IO[ErrorResponse, Entity] = {
    ZIO
      .validateFirst(credentials +: others)(authenticator.authenticate)
      .catchAll { errors =>
        val isAllMethodsDisabled = errors.forall {
          case AuthenticationMethodNotEnabled(_) => true
          case _                                 => false
        }

        if (isAllMethodsDisabled) ZIO.succeed(Entity.Default)
        else ZIO.fail(errors.head) // cannot fail, always non-empty
      }
      .mapError(AuthenticationError.toErrorResponse)
  }

  def securityLogic(credentials: (ApiKeyCredentials, JwtCredentials))(
      authenticator: Authenticator
  ): IO[ErrorResponse, Entity] =
    securityLogic(credentials._2, credentials._1)(authenticator)

}
