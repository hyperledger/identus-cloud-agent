package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.iam.authentication.admin
import io.iohk.atala.iam.authentication.admin.{AdminApiKeyAuthenticator, AdminApiKeyCredentials}
import io.iohk.atala.iam.authentication.apikey.*
import zio.*
import zio.ZIO.*
import zio.ZLayer.*

case class DefaultAuthenticator(
    adminApiKeyAuthenticator: AdminApiKeyAuthenticator,
    apiKeyAuthenticator: ApiKeyAuthenticator
) extends Authenticator {
  override def authenticate(credentials: Credentials): IO[AuthenticationError, Entity] = credentials match {
    case adminApiKeyCredentials: AdminApiKeyCredentials => adminApiKeyAuthenticator(adminApiKeyCredentials)
    case apiKeyCredentials: ApiKeyCredentials =>
      apiKeyAuthenticator(apiKeyCredentials)
        .catchSome { case AuthenticationMethodNotEnabled(_: String) =>
          ZIO.succeed(Entity.Default)
        }
  }
}

object DefaultAuthenticator {
  val layer: URLayer[AdminApiKeyAuthenticator & ApiKeyAuthenticator, Authenticator] =
    ZLayer.fromZIO {
      for {
        adminApiKeyAuthenticator <- ZIO.service[AdminApiKeyAuthenticator]
        apiKeyAuthenticator <- ZIO.service[ApiKeyAuthenticator]
      } yield DefaultAuthenticator(adminApiKeyAuthenticator, apiKeyAuthenticator)
    }
}
