package io.iohk.atala.iam.authentication.admin

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.AuthenticationError.*
import io.iohk.atala.iam.authentication.{AuthenticationError, Authenticator, Credentials}
import zio.{IO, ZIO}

trait AdminApiKeyAuthenticator extends Authenticator {

  def authenticate(credentials: Credentials): IO[AuthenticationError, Entity] = {
    credentials match {
      case EmptyAdminApiKeyCredentials() =>
        ZIO.logInfo("Admin API authentication enabled, but `x-admin-api` token is not provided") *>
          ZIO.fail(InvalidCredentials("Admin API key is not provided"))
      case AdminApiKeyCredentials(apiKey) => authenticate(apiKey)
    }
  }
  def authenticate(adminApiKey: String): IO[AuthenticationError, Entity]
}

object AdminApiKeyAuthenticator {
  // TODO: probably, we need to add the roles to the entities, for now, it works like this
  val Admin = Entity(name = "admin")
}
