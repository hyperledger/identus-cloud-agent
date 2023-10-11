package io.iohk.atala.iam.authentication

import io.iohk.atala.iam.authentication.admin.AdminConfig
import io.iohk.atala.iam.authentication.apikey.ApiKeyConfig
import io.iohk.atala.iam.authentication.oidc.KeycloakConfig

final case class AuthenticationConfig(
    admin: AdminConfig,
    apiKey: ApiKeyConfig,
    keycloak: KeycloakConfig
) {

  /** Return true if at least 1 authentication method is enabled (exlcuding admin auth method) */
  def isEnabledAny: Boolean = apiKey.enabled || keycloak.enabled

}
