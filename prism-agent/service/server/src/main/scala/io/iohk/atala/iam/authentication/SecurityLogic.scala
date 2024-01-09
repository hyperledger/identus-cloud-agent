package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.iam.authentication.admin.AdminApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletAdministrationContext
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
        val errorsExcludingMethodNotEnabled = errors.filter {
          case AuthenticationMethodNotEnabled(_) => false
          case _                                 => true
        }

        // if the alternative authentication method is not configured,
        // apikey authentication is disabled the default user is used
        errorsExcludingMethodNotEnabled match {
          case Nil       => ZIO.left(Entity.Default)
          case head :: _ => ZIO.fail(head)
        }
      }
      .mapError(AuthenticationError.toErrorResponse)
  }

  def authorize[E <: BaseEntity](entity: E)(authorizer: Authorizer[E]): IO[ErrorResponse, WalletAccessContext] =
    authorizer
      .authorizeWalletAccess(entity)
      .mapError(AuthenticationError.toErrorResponse)

  def authorize[E <: BaseEntity](credentials: Credentials, others: Credentials*)(
      authenticator: Authenticator[E],
      authorizer: Authorizer[E]
  ): IO[ErrorResponse, WalletAccessContext] =
    authenticate[E](credentials, others: _*)(authenticator)
      .flatMap {
        case Left(entity)  => authorize(entity)(EntityAuthorizer)
        case Right(entity) => authorize(entity)(authorizer)
      }

  def authorizeWith[E <: BaseEntity](credentials: (ApiKeyCredentials, JwtCredentials))(
      authenticator: Authenticator[E],
      authorizer: Authorizer[E]
  ): IO[ErrorResponse, WalletAccessContext] =
    authorize[E](credentials._2, credentials._1)(authenticator, authorizer)

  def authorizeWalletAdmin[E <: BaseEntity](
      entity: E
  )(authorizer: Authorizer[E]): IO[ErrorResponse, WalletAdministrationContext] =
    authorizer
      .authorizeWalletAdmin(entity)
      .mapError(AuthenticationError.toErrorResponse)

  def authorizeWalletAdminWith[E <: BaseEntity](
      credentials: (AdminApiKeyCredentials, ApiKeyCredentials, JwtCredentials)
  )(
      authenticator: Authenticator[E],
      authorizer: Authorizer[E]
  ): IO[ErrorResponse, (BaseEntity, WalletAdministrationContext)] =
    authenticate[E](credentials._1, credentials._3, credentials._2)(authenticator)
      .flatMap {
        case Left(entity)  => authorizeWalletAdmin(entity)(EntityAuthorizer).map(entity -> _)
        case Right(entity) => authorizeWalletAdmin(entity)(authorizer).map(entity -> _)
      }
}
