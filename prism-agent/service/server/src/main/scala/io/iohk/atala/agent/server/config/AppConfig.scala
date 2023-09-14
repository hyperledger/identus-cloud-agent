package io.iohk.atala.agent.server.config

import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.iam.authentication.AuthenticationConfig
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.shared.db.DbConfig
import zio.config.*
import zio.config.magnolia.Descriptor

import java.net.URL
import java.time.Duration

final case class AppConfig(
    devMode: Boolean,
    iris: IrisConfig,
    pollux: PolluxConfig,
    agent: AgentConfig,
    connect: ConnectConfig,
    prismNode: PrismNodeConfig,
) {
  def validate: Either[String, Unit] =
    for {
      _ <- agent.validate
    } yield ()
}

object AppConfig {
  val descriptor: ConfigDescriptor[AppConfig] = Descriptor[AppConfig]
}

final case class VaultConfig(address: String, token: String)

final case class IrisConfig(service: GrpcServiceConfig)

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
    connectBgJobProcessingParallelism: Int,
    connectInvitationExpiry: Duration,
)

final case class PrismNodeConfig(service: GrpcServiceConfig)

final case class GrpcServiceConfig(host: String, port: Int)

final case class DatabaseConfig(
    host: String,
    port: Int,
    databaseName: String,
    username: String,
    password: String,
    appUsername: String,
    appPassword: String,
    awaitConnectionThreads: Int
) {
  def dbConfig(appUser: Boolean): DbConfig = {
    DbConfig(
      username = if (appUser) appUsername else username,
      password = if (appUser) appPassword else password,
      jdbcUrl = s"jdbc:postgresql://${host}:${port}/${databaseName}",
      awaitConnectionThreads = awaitConnectionThreads
    )
  }
}

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
    url: Option[URL],
    apiKey: Option[String],
    parallelism: Option[Int]
)

final case class DefaultWalletConfig(
    enabled: Boolean,
    seed: Option[String],
    webhookUrl: Option[URL],
    webhookApiKey: Option[String],
    authApiKey: String
)

final case class AgentConfig(
    httpEndpoint: HttpEndpointConfig,
    authentication: AuthenticationConfig,
    didCommServiceEndpointUrl: String,
    database: DatabaseConfig,
    verification: VerificationConfig,
    secretStorage: SecretStorageConfig,
    webhookPublisher: WebhookPublisherConfig,
    defaultWallet: DefaultWalletConfig
) {
  def validate: Either[String, Unit] = {
    if (!defaultWallet.enabled && !authentication.apiKey.enabled)
      Left("The default wallet cannot be disabled if the apikey authentication is disabled.")
    else
      Right(())
  }
}

final case class HttpEndpointConfig(http: HttpConfig)

final case class HttpConfig(port: Int)

final case class SecretStorageConfig(
    backend: String,
    vault: Option[VaultConfig],
)
