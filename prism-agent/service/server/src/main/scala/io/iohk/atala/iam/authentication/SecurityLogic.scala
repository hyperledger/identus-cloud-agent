package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

object SecurityLogic {

  def authenticate[E <: BaseEntity](credentials: Credentials, others: Credentials*)(
      authenticator: Authenticator[E],
  ): IO[ErrorResponse, Either[Entity, E]] = {
    val creds = credentials :: others.toList
    ZIO
      .validateFirst(creds)(authenticator.authenticate)
      .map(Right(_))
      .catchAll { errors =>
        val isAllMethodsDisabled = errors.forall {
          case AuthenticationMethodNotEnabled(_) => true
          case _                                 => false
        }

        // if the alternative authentication method is not configured,
        // apikey authentication is disabled the default user is used
        if (isAllMethodsDisabled) ZIO.left(Entity.Default)
        else ZIO.fail(errors.head) // cannot fail, always non-empty
      }
      .mapError(AuthenticationError.toErrorResponse)
  }

  def authorize[E <: BaseEntity](credentials: Credentials, others: Credentials*)(
      authenticator: Authenticator[E] & Authorizer[E],
  ): IO[ErrorResponse, WalletAccessContext] = {
    authenticate[E](credentials, others: _*)(authenticator)
      .flatMap {
        case Left(entity) =>
          EntityAuthorizer
            .authorize(entity)
            .mapError(AuthenticationError.toErrorResponse)
        case Right(entity) =>
          authenticator
            .authorize(entity)
            .mapError(AuthenticationError.toErrorResponse)
      }
      .map(walletId => WalletAccessContext(walletId))
  }

  def authorizeWith[E <: BaseEntity](credentials: (ApiKeyCredentials, JwtCredentials))(
      authenticator: Authenticator[E] & Authorizer[E]
  ): IO[ErrorResponse, WalletAccessContext] =
    authorize[E](credentials._2, credentials._1)(authenticator)

}
