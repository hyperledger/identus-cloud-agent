package io.iohk.atala.agent.server.config

import zio.config.*
import zio.config.magnolia.Descriptor
import java.time.Duration

final case class AppConfig(
    iris: IrisConfig,
    castor: CastorConfig,
    pollux: PolluxConfig,
    agent: AgentConfig,
    connect: ConnectConfig,
    prismNode: PrismNodeConfig
)

object AppConfig {
  val descriptor: ConfigDescriptor[AppConfig] = Descriptor[AppConfig]
}

final case class IrisConfig(service: GrpcServiceConfig)

final case class CastorConfig(database: DatabaseConfig)
final case class PolluxConfig(
    database: DatabaseConfig,
    issueBgJobRecurrenceDelay: Duration,
    presentationBgJobRecurrenceDelay: Duration
)
final case class ConnectConfig(database: DatabaseConfig, connectBgJobRecurrenceDelay: Duration)

final case class PrismNodeConfig(service: GrpcServiceConfig)

final case class GrpcServiceConfig(host: String, port: Int)

final case class DatabaseConfig(
    host: String,
    port: Int,
    databaseName: String,
    username: String,
    password: String,
    awaitConnectionThreads: Int
)

final case class AgentConfig(
    httpEndpoint: HttpEndpointConfig,
    didCommServiceEndpointUrl: String,
    database: DatabaseConfig
)

final case class HttpEndpointConfig(http: HttpConfig)

final case class HttpConfig(port: Int)
