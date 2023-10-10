package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

object SecurityLogic {

  def walletAccessContext[E <: BaseEntity](credentials: Credentials, others: Credentials*)(
      authenticator: Authenticator[E] & Authorizer[E]
  ): IO[ErrorResponse, WalletAccessContext] = {
    val creds: List[Credentials] = credentials :: others.toList
    ZIO
      .validateFirst(creds) { cred =>
        authenticator
          .authenticate(cred)
          .flatMap(authenticator.authorize)
      }
      .map(walletId => WalletAccessContext(walletId))
      .catchAll { errors =>
        val isAllMethodsDisabled = errors.forall {
          case AuthenticationMethodNotEnabled(_) => true
          case _                                 => false
        }

        if (isAllMethodsDisabled) ZIO.succeed(WalletAccessContext(WalletId.default))
        else ZIO.fail(errors.head) // cannot fail, always non-empty
      }
      .mapError(AuthenticationError.toErrorResponse)
  }

  def walletAccessContextFrom[E <: BaseEntity](credentials: (ApiKeyCredentials, JwtCredentials))(
      authenticator: Authenticator[E] & Authorizer[E]
  ): IO[ErrorResponse, WalletAccessContext] =
    walletAccessContext[E](credentials._2, credentials._1)(authenticator)

}
