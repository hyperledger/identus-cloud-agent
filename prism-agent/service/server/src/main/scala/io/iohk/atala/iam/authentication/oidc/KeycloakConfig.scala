package io.iohk.atala.iam.authentication.oidc

import zio.*
import io.iohk.atala.agent.server.config.AppConfig
import java.net.URL

final case class KeycloakConfig(
    enabled: Boolean,
    keycloakUrl: URL,
    realmName: String,
    clientId: String,
    clientSecret: String,
    autoUpgradeToRPT: Boolean
)

object KeycloakConfig {
  val layer: URLayer[AppConfig, KeycloakConfig] =
    ZLayer.fromFunction((conf: AppConfig) => conf.agent.authentication.keycloak)
}
