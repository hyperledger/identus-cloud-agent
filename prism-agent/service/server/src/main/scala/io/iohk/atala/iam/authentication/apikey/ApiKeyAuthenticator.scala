package io.iohk.atala.iam.authentication.apikey

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.{AuthenticationError, Authenticator, Credentials}
import AuthenticationError.*
import zio.{IO, ZIO}

import java.util.UUID

trait ApiKeyAuthenticator extends Authenticator {

  def authenticate(credentials: Credentials): IO[AuthenticationError, Entity] = {
    credentials match {
      case ApiKeyCredentials(apiKey) =>
        apiKey match {
          case Some(value) if value.nonEmpty => authenticate(value)
          case Some(value) =>
            ZIO.logInfo(s"ApiKey API authentication is enabled, but `api-key` token is empty") *>
              ZIO.fail(ApiKeyAuthenticationError.emptyApiKey)
          case None =>
            ZIO.logInfo(s"ApiKey API authentication is enabled, but `api-key` token is not provided") *>
              ZIO.fail(InvalidCredentials("ApiKey key is not provided"))
        }
      case other =>
        ZIO.fail(InvalidCredentials("ApiKey key is not provided"))
    }
  }
  def authenticate(apiKey: String): IO[AuthenticationError, Entity]

  def add(entityId: UUID, apiKey: String): IO[AuthenticationError, Unit]
}
