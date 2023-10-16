package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.shared.models.WalletId
import zio.{IO, ZIO, ZLayer}

trait Credentials

trait AuthenticationError {
  def message: String
}

object AuthenticationError {

  case class InvalidCredentials(message: String) extends AuthenticationError

  case class AuthenticationMethodNotEnabled(message: String) extends AuthenticationError

  case class UnexpectedError(message: String) extends AuthenticationError

  case class ServiceError(message: String) extends AuthenticationError

  case class ResourceNotPermitted(message: String) extends AuthenticationError

  def toErrorResponse(error: AuthenticationError): ErrorResponse =
    ErrorResponse(
      status = sttp.model.StatusCode.Forbidden.code,
      `type` = "authentication_error",
      title = "",
      detail = Option(error.message),
      instance = ""
    )
}

trait Authenticator[E <: BaseEntity] {
  def authenticate(credentials: Credentials): IO[AuthenticationError, E]

  def isEnabled: Boolean

  def apply(credentials: Credentials): IO[AuthenticationError, E] = authenticate(credentials)
}

trait Authorizer[E <: BaseEntity] {
  def authorize(entity: E): IO[AuthenticationError, WalletId]
}

object EntityAuthorizer extends EntityAuthorizer

trait EntityAuthorizer extends Authorizer[Entity] {
  override def authorize(entity: Entity): IO[AuthenticationError, WalletId] =
    ZIO.succeed(entity.walletId).map(WalletId.fromUUID)
}

trait AuthenticatorWithAuthZ[E <: BaseEntity] extends Authenticator[E], Authorizer[E]

object DefaultEntityAuthenticator extends AuthenticatorWithAuthZ[BaseEntity] {

  override def isEnabled: Boolean = true
  override def authenticate(credentials: Credentials): IO[AuthenticationError, BaseEntity] = ZIO.succeed(Entity.Default)
  override def authorize(entity: BaseEntity): IO[AuthenticationError, WalletId] =
    EntityAuthorizer.authorize(Entity.Default)

  val layer = ZLayer.apply(ZIO.succeed(DefaultEntityAuthenticator))
}
