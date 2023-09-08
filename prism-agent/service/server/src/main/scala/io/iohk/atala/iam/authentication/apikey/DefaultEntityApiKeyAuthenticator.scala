package io.iohk.atala.iam.authentication.apikey

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.AuthenticationError
import zio.{IO, ZIO}

import java.util.UUID

object DefaultEntityApiKeyAuthenticator extends ApiKeyAuthenticator {
  private val DefaultEntity = Entity(name = "default")
  override def authenticate(apiKey: String): IO[AuthenticationError, Entity] = {
    ZIO.succeed(DefaultEntity)
  }

  override def add(entityId: UUID, apiKey: String): IO[AuthenticationError, Unit] = ZIO.succeed(())

  override def delete(entityId: UUID, apiKey: String): IO[AuthenticationError, Unit] = ZIO.succeed(())

  override def isEnabled = true
}
