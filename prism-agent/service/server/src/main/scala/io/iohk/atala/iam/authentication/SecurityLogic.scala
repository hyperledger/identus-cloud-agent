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
    val head = authenticator.authenticate(credentials)
    val tail = others.map(authenticator.authenticate)
    ZIO
      .firstSuccessOf(head, tail)
      .catchSome { case AuthenticationMethodNotEnabled(_) =>
        ZIO.succeed(Entity.Default)
      }
      .mapError(AuthenticationError.toErrorResponse)
  }

  def securityLogic(credentials: (ApiKeyCredentials, JwtCredentials))(
      authenticator: Authenticator
  ): IO[ErrorResponse, Entity] =
    securityLogic(credentials._1, credentials._2)(authenticator)

}
