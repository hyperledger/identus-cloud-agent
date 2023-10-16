package io.iohk.atala.iam.authentication.apikey

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.AuthenticationError.*
import io.iohk.atala.iam.authentication.AuthenticatorWithAuthZ
import io.iohk.atala.iam.authentication.EntityAuthorizer
import io.iohk.atala.iam.authentication.{AuthenticationError, Credentials}
import zio.{IO, ZIO}

import java.util.UUID

trait ApiKeyAuthenticator extends AuthenticatorWithAuthZ[Entity], EntityAuthorizer {

  def authenticate(credentials: Credentials): IO[AuthenticationError, Entity] = {
    if (isEnabled) {
      credentials match {
        case ApiKeyCredentials(apiKey) =>
          apiKey match {
            case Some(value) if value.nonEmpty => authenticate(value)
            case Some(value) =>
              ZIO.logInfo(s"ApiKey API authentication is enabled, but `apikey` token is empty") *>
                ZIO.fail(ApiKeyAuthenticationError.emptyApiKey)
            case None =>
              ZIO.logInfo(s"ApiKey API authentication is enabled, but `apikey` token is not provided") *>
                ZIO.fail(InvalidCredentials("ApiKey key is not provided"))
          }
        case other =>
          ZIO.fail(InvalidCredentials("ApiKey key is not provided"))
      }
    } else ZIO.fail(AuthenticationMethodNotEnabled("ApiKey API authentication is not enabled"))
  }

  def isEnabled: Boolean

  def authenticate(apiKey: String): IO[AuthenticationError, Entity]

  def add(entityId: UUID, apiKey: String): IO[AuthenticationError, Unit]

  def delete(entityId: UUID, apiKey: String): IO[AuthenticationError, Unit]
}

object ApiKeyAuthenticator {
  def authenticate(credentials: Credentials): ZIO[ApiKeyAuthenticator, AuthenticationError, Entity] =
    ZIO.serviceWithZIO[ApiKeyAuthenticator](_.authenticate(credentials))

  def add(entityId: UUID, apiKey: String): ZIO[ApiKeyAuthenticator, AuthenticationError, Unit] =
    ZIO.serviceWithZIO[ApiKeyAuthenticator](_.add(entityId, apiKey))
}
