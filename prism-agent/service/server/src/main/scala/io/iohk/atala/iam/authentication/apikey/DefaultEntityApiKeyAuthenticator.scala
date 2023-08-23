package io.iohk.atala.iam.authentication.apikey

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.AuthenticationError
import zio.{IO, ZIO}

object DefaultEntityApiKeyAuthenticator extends ApiKeyAuthenticator {
  private val DefaultEntity = Entity(name = "default")
  override def authenticate(apiKey: String): IO[AuthenticationError, Entity] = {
    ZIO.succeed(DefaultEntity)
  }
}
