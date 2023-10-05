package io.iohk.atala.iam.authentication.keycloak

import zio.*
import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.agent.walletapi.model.Entity

class KeycloakAuthenticatorImpl(keycloakConfig: KeycloakConfig) extends KeycloakAuthenticator {

  // TODO: implement
  override def authenticate(token: String): IO[AuthenticationError, Entity] = ???

  override def isEnabled: Boolean = keycloakConfig.enabled

}

object KeycloakAuthenticatorImpl {
  val layer: URLayer[KeycloakConfig, KeycloakAuthenticator] = ZLayer.fromFunction(KeycloakAuthenticatorImpl(_))
}
