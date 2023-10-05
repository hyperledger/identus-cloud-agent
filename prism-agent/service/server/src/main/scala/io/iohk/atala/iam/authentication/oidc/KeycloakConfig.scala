package io.iohk.atala.iam.authentication.oidc

import zio.*
import io.iohk.atala.agent.server.config.AppConfig

final case class KeycloakConfig(enabled: Boolean, clientId: String, clientSecret: String)

object KeycloakConfig {
  val layer: URLayer[AppConfig, KeycloakConfig] =
    ZLayer.fromFunction((conf: AppConfig) => conf.agent.authentication.keycloak)
}
