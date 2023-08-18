package io.iohk.atala.iam.authentication.admin

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.{AuthenticationError, Authenticator, Credentials, InvalidCredentials}
import zio.{IO, ZIO}
import io.iohk.atala.iam.authentication.admin.AdminConfig

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
  val Admin = Entity(name = "admin")
}

class AdminApiKeyAuthenticatorImpl(adminConfig: AdminConfig) extends AdminApiKeyAuthenticator {
  def authenticate(adminApiKey: String): IO[AuthenticationError, Entity] = {
    if (adminApiKey == adminConfig.token) {
      ZIO.logInfo(s"Admin API key authentication successful") *>
        ZIO.succeed(Admin)
    } else ZIO.fail(AdminApiKeyAuthenticationError.invalidAdminApiKey)
  }
}
