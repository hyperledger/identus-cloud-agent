package org.hyperledger.identus.agent.server

import org.hyperledger.identus.agent.notification.WebhookPublisher
import org.hyperledger.identus.agent.server.config.AppConfig
import org.hyperledger.identus.agent.server.http.{ZHttp4sBlazeServer, ZHttpEndpoints}
import org.hyperledger.identus.agent.server.jobs.*
import org.hyperledger.identus.agent.walletapi.model.{Entity, Wallet, WalletSeed}
import org.hyperledger.identus.agent.walletapi.service.{EntityService, ManagedDIDService, WalletManagementService}
import org.hyperledger.identus.castor.controller.{DIDRegistrarServerEndpoints, DIDServerEndpoints}
import org.hyperledger.identus.connect.controller.ConnectionServerEndpoints
import org.hyperledger.identus.credentialstatus.controller.CredentialStatusServiceEndpoints
import org.hyperledger.identus.event.controller.EventServerEndpoints
import org.hyperledger.identus.event.notification.EventNotificationConfig
import org.hyperledger.identus.iam.authentication.apikey.ApiKeyAuthenticator
import org.hyperledger.identus.iam.entity.http.EntityServerEndpoints
import org.hyperledger.identus.iam.wallet.http.WalletManagementServerEndpoints
import org.hyperledger.identus.issue.controller.IssueServerEndpoints
import org.hyperledger.identus.messaging.MessagingService
import org.hyperledger.identus.messaging.MessagingService.RetryStep
import org.hyperledger.identus.oid4vci.CredentialIssuerServerEndpoints
import org.hyperledger.identus.pollux.credentialdefinition.CredentialDefinitionRegistryServerEndpoints
import org.hyperledger.identus.pollux.credentialschema.{
  SchemaRegistryServerEndpoints,
  VerificationPolicyServerEndpoints
}
import org.hyperledger.identus.presentproof.controller.PresentProofServerEndpoints
import org.hyperledger.identus.shared.models.{HexString, WalletAccessContext, WalletAdministrationContext, WalletId, Serde}
import org.hyperledger.identus.shared.utils.DurationOps.toMetricsSeconds
import org.hyperledger.identus.system.controller.SystemServerEndpoints
import org.hyperledger.identus.verification.controller.VcVerificationServerEndpoints
import zio.*
import zio.metrics.*
import org.hyperledger.identus.messaging.*
object CloudAgentApp {

  def run = for {
    _ <- AgentInitialization.run
    _ <- issueCredentialExchangesJob
    _ <- presentProofExchangeJob
    _ <- connectDidCommExchangesJob
    _ <- syncDIDPublicationStateFromDltJob.debug.fork
    _ <- consumeAndSyncDIDPublicationStateFromDlt
    _ <- syncRevocationStatusListsJob.debug.fork
    _ <- AgentHttpServer.run.tapDefect(e => ZIO.logErrorCause("Agent HTTP Server failure", e)).fork
    fiber <- DidCommHttpServer.run.tapDefect(e => ZIO.logErrorCause("DIDComm HTTP Server failure", e)).fork
    _ <- WebhookPublisher.layer.build.map(_.get[WebhookPublisher]).flatMap(_.run.debug.fork)
    _ <- fiber.join *> ZIO.log(s"Server End")
    _ <- ZIO.never
  } yield ()

  private val presentProofExchangeJob = MessagingService.consumeWithRetryStrategy(
    "identus-cloud-agent",
    PresentBackgroundJobs.handleMessage,
    Seq(
      RetryStep("present-proof", 5, 0.seconds, "present-proof-retry-1"),
      RetryStep("present-proof-retry-1", 5, 2.seconds, "present-proof-retry-2"),
      RetryStep("present-proof-retry-2", 5, 4.seconds, "present-proof-retry-3"),
      RetryStep("present-proof-retry-3", 5, 8.seconds, "present-proof-retry-4"),
      RetryStep("present-proof-retry-4", 5, 16.seconds, "present-proof-DLQ")
    )
  )
  // TODO  @@ Metric
  //        .gauge("present_proof_flow_did_com_exchange_job_ms_gauge")
  //        .trackDurationWith(_.toMetricsSeconds))
  //        .repeat(Schedule.spaced(config.pollux.presentationBgJobRecurrenceDelay))
  //        .unit
  private val issueCredentialExchangesJob = MessagingService.consumeWithRetryStrategy(
    "identus-cloud-agent",
    IssueBackgroundJobs.handleMessage,
    Seq(
      RetryStep("issue-credential", 5, 0.seconds, "issue-credential-retry-1"),
      RetryStep("issue-credential-retry-1", 5, 2.seconds, "issue-credential-retry-2"),
      RetryStep("issue-credential-retry-2", 5, 4.seconds, "issue-credential-retry-3"),
      RetryStep("issue-credential-retry-3", 5, 8.seconds, "issue-credential-retry-4"),
      RetryStep("issue-credential-retry-4", 5, 16.seconds, "issue-credential-DLQ")
    )
  )
  // TODO See how metrics can be re-implemented using MessagingService
  //      _ <- (IssueBackgroundJobs.issueCredentialDidCommExchanges @@ Metric
  //        .gauge("issuance_flow_did_com_exchange_job_ms_gauge")
  //        .trackDurationWith(_.toMetricsSeconds))
  //        .repeat(Schedule.spaced(config.pollux.issueBgJobRecurrenceDelay))
  //        .unit
  private val connectDidCommExchangesJob = MessagingService.consumeWithRetryStrategy(
    "identus-cloud-agent",
    ConnectBackgroundJobs.handleMessage,
    Seq(
      RetryStep("connect", 5, 0.seconds, "connect-retry-1"),
      RetryStep("connect-retry-1", 5, 2.seconds, "connect-retry-2"),
      RetryStep("connect-retry-2", 5, 4.seconds, "connect-retry-3"),
      RetryStep("connect-retry-3", 5, 8.seconds, "connect-retry-4"),
      RetryStep("connect-retry-4", 5, 16.seconds, "connect-DLQ")
    )
  )

  // TODO See how metrics can be re-implemented using MessagingService
  //      _ <- (ConnectBackgroundJobs.didCommExchanges @@ Metric
  //        .gauge("connection_flow_did_com_exchange_job_ms_gauge")
  //        .trackDurationWith(_.toMetricsSeconds))
  //        .repeat(Schedule.spaced(config.connect.connectBgJobRecurrenceDelay))
  //        .unit

  private val syncRevocationStatusListsJob = {
    for {
      config <- ZIO.service[AppConfig]
      _ <- (StatusListJobs.syncRevocationStatuses @@ Metric
        .gauge("revocation_status_list_sync_job_ms_gauge")
        .trackDurationWith(_.toMetricsSeconds))
        .repeat(Schedule.spaced(config.pollux.syncRevocationStatusesBgJobRecurrenceDelay))
    } yield ()
  }

  private val syncDIDPublicationStateFromDltJob: URIO[ManagedDIDService & WalletManagementService & Producer[WalletId, WalletId], Unit] =
    ZIO
      .serviceWithZIO[WalletManagementService](_.listWallets().map(_._1))
      .flatMap { wallets =>
        ZIO.foreach(wallets) { wallet =>
          for {
            producer <- ZIO.service[Producer[WalletId, WalletId]]
            _ <- producer.produce("sync-did-state", wallet.id, wallet.id)
          } yield ()
        }
      }
      .catchAll(e => ZIO.logError(s"error while syncing DID publication state: $e"))
      .repeat(Schedule.spaced(10.seconds))
      .unit
      .provideSomeLayer(ZLayer.succeed(WalletAdministrationContext.Admin()))

  
  private val consumeAndSyncDIDPublicationStateFromDlt = MessagingService.consumeStrategy(
    groupId = "identus-cloud-agent",
    topicName = "sync-did-state", 
    consumerCount = 5,
    DIDStateSyncBackgroundJobs.handleMessage
  ) 
}

object AgentHttpServer {
  val agentRESTServiceEndpoints = for {
    allCredentialDefinitionRegistryEndpoints <- CredentialDefinitionRegistryServerEndpoints.all
    allSchemaRegistryEndpoints <- SchemaRegistryServerEndpoints.all
    allVerificationPolicyEndpoints <- VerificationPolicyServerEndpoints.all
    allConnectionEndpoints <- ConnectionServerEndpoints.all
    allIssueEndpoints <- IssueServerEndpoints.all
    allStatusListEndpoints <- CredentialStatusServiceEndpoints.all
    allDIDEndpoints <- DIDServerEndpoints.all
    allDIDRegistrarEndpoints <- DIDRegistrarServerEndpoints.all
    allPresentProofEndpoints <- PresentProofServerEndpoints.all
    allVcVerificationEndpoints <- VcVerificationServerEndpoints.all
    allSystemEndpoints <- SystemServerEndpoints.all
    allEntityEndpoints <- EntityServerEndpoints.all
    allWalletManagementEndpoints <- WalletManagementServerEndpoints.all
    allEventEndpoints <- EventServerEndpoints.all
    allOIDCEndpoints <- CredentialIssuerServerEndpoints.all
  } yield allCredentialDefinitionRegistryEndpoints ++
    allSchemaRegistryEndpoints ++
    allVerificationPolicyEndpoints ++
    allConnectionEndpoints ++
    allDIDEndpoints ++
    allDIDRegistrarEndpoints ++
    allIssueEndpoints ++
    allStatusListEndpoints ++
    allPresentProofEndpoints ++
    allVcVerificationEndpoints ++
    allSystemEndpoints ++
    allEntityEndpoints ++
    allWalletManagementEndpoints ++
    allEventEndpoints ++
    allOIDCEndpoints
  def run =
    for {
      allEndpoints <- agentRESTServiceEndpoints
      allEndpointsWithDocumentation = ZHttpEndpoints.withDocumentations[Task](allEndpoints)
      server <- ZHttp4sBlazeServer.make("rest_api")
      appConfig <- ZIO.service[AppConfig]
      _ <- server.start(allEndpointsWithDocumentation, port = appConfig.agent.httpEndpoint.http.port).debug
    } yield ()
}

object AgentInitialization {

  private val defaultWalletId = WalletId.default
  private val defaultWallet = Wallet("default", defaultWalletId)
  private val defaultEntity = Entity.Default

  def run: RIO[AppConfig & WalletManagementService & EntityService & ApiKeyAuthenticator, Unit] =
    for {
      _ <- validateAppConfig
      _ <- initializeDefaultWallet
        .provideSomeLayer(ZLayer.succeed(WalletAdministrationContext.Admin()))
    } yield ()

  private val validateAppConfig =
    ZIO.serviceWithZIO[AppConfig](conf =>
      ZIO
        .fromEither(conf.validate)
        .mapError(msg => RuntimeException(s"Application configuration is invalid. $msg"))
    )

  private val initializeDefaultWallet =
    for {
      _ <- ZIO.logInfo("Initializing default wallet.")
      config <- ZIO.serviceWith[AppConfig](_.agent.defaultWallet)
      walletService <- ZIO.service[WalletManagementService]
      isDefaultWalletEnabled = config.enabled
      isDefaultWalletExist <- walletService
        .findWallet(defaultWalletId)
        .map(_.isDefined)
      _ <- ZIO.logInfo(s"Default wallet not enabled.").when(!isDefaultWalletEnabled)
      _ <- ZIO.logInfo(s"Default wallet already exist.").when(isDefaultWalletExist)
      _ <- createDefaultWallet.when(isDefaultWalletEnabled && !isDefaultWalletExist)
    } yield ()

  private val createDefaultWallet =
    for {
      walletService <- ZIO.service[WalletManagementService]
      entityService <- ZIO.service[EntityService]
      apiKeyAuth <- ZIO.service[ApiKeyAuthenticator]
      config <- ZIO.serviceWith[AppConfig](_.agent.defaultWallet)
      seed <- config.seed.fold(ZIO.none) { seedHex =>
        ZIO
          .fromTry(HexString.fromString(seedHex))
          .map(bytes => WalletSeed.fromByteArray(bytes.toByteArray).left.map(Exception(_)))
          .absolve
          .asSome
      }
      _ <- ZIO.logInfo(s"Default wallet seed is not provided. New seed will be generated.").when(seed.isEmpty)
      _ <- walletService
        .createWallet(defaultWallet, seed)
        .orDieAsUnmanagedFailure
      _ <- entityService.create(defaultEntity).orDieAsUnmanagedFailure
      _ <- apiKeyAuth.add(defaultEntity.id, config.authApiKey)
      _ <- config.webhookUrl.fold(ZIO.unit) { url =>
        val customHeaders = config.webhookApiKey.fold(Map.empty)(apiKey => Map("Authorization" -> s"Bearer $apiKey"))
        walletService
          .createWalletNotification(EventNotificationConfig(defaultWalletId, url, customHeaders))
          .orDieAsUnmanagedFailure
          .provide(ZLayer.succeed(WalletAccessContext(defaultWalletId)))
      }
    } yield ()

}
