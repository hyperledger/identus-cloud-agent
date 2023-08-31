package io.iohk.atala.iam.authentication.apikey

import io.iohk.atala.agent.server.config.AppConfig
import zio.{URLayer, ZLayer}
import zio.ZIO

case class ApiKeyConfig(salt: String, enabled: Boolean, authenticateAsDefaultUser: Boolean, autoProvisioning: Boolean)

object ApiKeyConfig {
  val layer: URLayer[AppConfig, ApiKeyConfig] =
    ZLayer.fromZIO(ZIO.service[AppConfig].map(_.agent.authentication.apiKey))

}
