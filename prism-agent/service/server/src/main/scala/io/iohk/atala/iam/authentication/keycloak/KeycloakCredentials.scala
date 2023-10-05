package io.iohk.atala.iam.authentication.keycloak

import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.iam.authentication.Credentials

final case class KeycloakCredentials(token: String) extends Credentials

final case class KeycloakAuthenticationError(message: String) extends AuthenticationError

object KeycloakAuthenticationError {
  val emptyToken = KeycloakAuthenticationError("Empty bearer token header provided")
}
