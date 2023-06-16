package io.iohk.atala.agent.server.config

import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.pollux.vc.jwt.*
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
    issueBgJobRecordsLimit: Int,
    issueBgJobRecurrenceDelay: Duration,
    issueBgJobProcessingParallelism: Int,
    presentationBgJobRecordsLimit: Int,
    presentationBgJobRecurrenceDelay: Duration,
    presentationBgJobProcessingParallelism: Int,
)
final case class ConnectConfig(
    database: DatabaseConfig,
    connectBgJobRecordsLimit: Int,
    connectBgJobRecurrenceDelay: Duration,
    connectBgJobProcessingParallelism: Int
)

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

final case class PresentationVerificationConfig(
    verifySignature: Boolean,
    verifyDates: Boolean,
    verifyHoldersBinding: Boolean,
    leeway: Duration,
)

final case class CredentialVerificationConfig(
    verifySignature: Boolean,
    verifyDates: Boolean,
    leeway: Duration,
)

final case class Options(credential: CredentialVerificationConfig, presentation: PresentationVerificationConfig)

final case class VerificationConfig(options: Options) {
  def toPresentationVerificationOptions(): JwtPresentation.PresentationVerificationOptions = {
    JwtPresentation.PresentationVerificationOptions(
      maybeProofPurpose = Some(VerificationRelationship.Authentication),
      verifySignature = options.presentation.verifySignature,
      verifyDates = options.presentation.verifyDates,
      verifyHoldersBinding = options.presentation.verifyHoldersBinding,
      leeway = options.presentation.leeway,
      maybeCredentialOptions = Some(
        CredentialVerification.CredentialVerificationOptions(
          verifySignature = options.credential.verifySignature,
          verifyDates = options.credential.verifyDates,
          leeway = options.credential.leeway,
          maybeProofPurpose = Some(VerificationRelationship.AssertionMethod)
        )
      )
    )
  }
}

final case class WebhookPublisherConfig(
    url: Option[String],
    apiKey: Option[String],
    parallelism: Option[Int]
)

final case class AgentConfig(
    httpEndpoint: HttpEndpointConfig,
    didCommServiceEndpointUrl: String,
    database: DatabaseConfig,
    verification: VerificationConfig,
    webhookPublisher: WebhookPublisherConfig
)

final case class HttpEndpointConfig(http: HttpConfig)

final case class HttpConfig(port: Int)
