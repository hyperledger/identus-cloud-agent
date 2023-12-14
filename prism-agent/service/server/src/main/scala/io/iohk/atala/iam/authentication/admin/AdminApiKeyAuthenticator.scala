package io.iohk.atala.iam.authentication.admin

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.AuthenticatorWithAuthZ
import io.iohk.atala.iam.authentication.EntityAuthorizer
import io.iohk.atala.iam.authentication.{AuthenticationError, Credentials}
import zio.*

trait AdminApiKeyAuthenticator extends AuthenticatorWithAuthZ[Entity], EntityAuthorizer {

  def authenticate(credentials: Credentials): IO[AuthenticationError, Entity] = {
    credentials match {
      case AdminApiKeyCredentials(Some(apiKey)) => authenticate(apiKey)
      case AdminApiKeyCredentials(None) =>
        ZIO.logInfo(s"AdminApiKey API authentication is enabled, but `x-admin-api-key` token is empty") *>
          ZIO.fail(AdminApiKeyAuthenticationError.emptyAdminApiKey)
    }
  }
  def authenticate(adminApiKey: String): IO[AuthenticationError, Entity]
}
