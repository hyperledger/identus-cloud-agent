package io.iohk.atala.iris.server.config

import zio.config.*
import zio.config.magnolia.Descriptor

final case class AppConfig(
    iris: IrisConfig
)

object AppConfig {
  val descriptor: ConfigDescriptor[AppConfig] = Descriptor[AppConfig]
}

final case class IrisConfig(database: DatabaseConfig)

final case class DatabaseConfig(host: String, port: Int, databaseName: String, username: String, password: String)
