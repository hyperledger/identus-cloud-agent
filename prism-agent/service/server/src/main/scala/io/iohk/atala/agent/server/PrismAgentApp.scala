package io.iohk.atala.agent.server

import io.iohk.atala.agent.notification.WebhookPublisher
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.http.{ZHttp4sBlazeServer, ZHttpEndpoints}
import io.iohk.atala.agent.server.jobs.{BackgroundJobs, ConnectBackgroundJobs}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.castor.controller.{DIDRegistrarServerEndpoints, DIDServerEndpoints}
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.connect.controller.ConnectionServerEndpoints
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.issue.controller.IssueServerEndpoints
import io.iohk.atala.mercury.DidOps
import io.iohk.atala.mercury.HttpClient
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.core.service.PresentationService
import io.iohk.atala.pollux.credentialschema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.credentialschema.VerificationPolicyServerEndpoints
import io.iohk.atala.pollux.vc.jwt.DidResolver as JwtDidResolver
import io.iohk.atala.presentproof.controller.PresentProofServerEndpoints
import io.iohk.atala.resolvers.DIDResolver
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.system.controller.SystemServerEndpoints
import zio.*

object PrismAgentApp {

  def run(didCommServicePort: Int) = for {
    _ <- issueCredentialDidCommExchangesJob.debug.fork
    _ <- presentProofExchangeJob.debug.fork
    _ <- connectDidCommExchangesJob.debug.fork
    _ <- syncDIDPublicationStateFromDltJob.fork
    _ <- AgentHttpServer.run.fork
    fiber <- DidCommHttpServer.run(didCommServicePort).fork
    _ <- WebhookPublisher.layer.build.map(_.get[WebhookPublisher]).flatMap(_.run.debug.fork)
    _ <- fiber.join *> ZIO.log(s"Server End")
    _ <- ZIO.never
  } yield ()

  private val issueCredentialDidCommExchangesJob: RIO[
    AppConfig & DidOps & DIDResolver & JwtDidResolver & HttpClient & CredentialService & DIDService &
      ManagedDIDService & PresentationService & WalletManagementService,
    Unit
  ] =
    for {
      config <- ZIO.service[AppConfig]
      _ <- ZIO
        .serviceWithZIO[WalletManagementService](_.listWallets)
        .flatMap { wallets =>
          ZIO.foreach(wallets) { wallet =>
            BackgroundJobs.issueCredentialDidCommExchanges
              .provideSomeLayer(ZLayer.succeed(WalletAccessContext(wallet)))
          }
        }
        .repeat(Schedule.spaced(config.pollux.issueBgJobRecurrenceDelay))
        .unit
    } yield ()

  private val presentProofExchangeJob: RIO[
    AppConfig & DidOps & DIDResolver & JwtDidResolver & HttpClient & PresentationService & CredentialService &
      DIDService & ManagedDIDService & WalletManagementService,
    Unit
  ] =
    for {
      config <- ZIO.service[AppConfig]
      _ <- ZIO
        .serviceWithZIO[WalletManagementService](_.listWallets)
        .flatMap { wallets =>
          ZIO.foreach(wallets) { wallet =>
            BackgroundJobs.presentProofExchanges
              .provideSomeLayer(ZLayer.succeed(WalletAccessContext(wallet)))
          }
        }
        .repeat(Schedule.spaced(config.pollux.presentationBgJobRecurrenceDelay))
        .unit
    } yield ()

  private val connectDidCommExchangesJob: RIO[
    AppConfig & DidOps & DIDResolver & HttpClient & ConnectionService & ManagedDIDService & WalletManagementService,
    Unit
  ] =
    for {
      config <- ZIO.service[AppConfig]
      _ <- ZIO
        .serviceWithZIO[WalletManagementService](_.listWallets)
        .flatMap { wallets =>
          ZIO.foreach(wallets) { wallet =>
            ConnectBackgroundJobs.didCommExchanges
              .provideSomeLayer(ZLayer.succeed(WalletAccessContext(wallet)))
          }
        }
        .repeat(Schedule.spaced(config.connect.connectBgJobRecurrenceDelay))
        .unit
    } yield ()

  private val syncDIDPublicationStateFromDltJob: URIO[ManagedDIDService & WalletManagementService, Unit] =
    ZIO
      .serviceWithZIO[WalletManagementService](_.listWallets)
      .flatMap { wallets =>
        ZIO.foreach(wallets) { wallet =>
          BackgroundJobs.syncDIDPublicationStateFromDlt
            .provideSomeLayer(ZLayer.succeed(WalletAccessContext(wallet)))
        }
      }
      .catchAll(e => ZIO.logError(s"error while syncing DID publication state: $e"))
      .repeat(Schedule.spaced(10.seconds))
      .unit

}

object AgentHttpServer {
  def run =
    for {
      allSchemaRegistryEndpoints <- SchemaRegistryServerEndpoints.all
      allVerificationPolicyEndpoints <- VerificationPolicyServerEndpoints.all
      allConnectionEndpoints <- ConnectionServerEndpoints.all
      allIssueEndpoints <- IssueServerEndpoints.all
      allDIDEndpoints <- DIDServerEndpoints.all
      allDIDRegistrarEndpoints <- DIDRegistrarServerEndpoints.all
      allPresentProofEndpoints <- PresentProofServerEndpoints.all
      allSystemEndpoints <- SystemServerEndpoints.all
      allEndpoints = ZHttpEndpoints.withDocumentations[Task](
        allSchemaRegistryEndpoints ++
          allVerificationPolicyEndpoints ++
          allConnectionEndpoints ++
          allDIDEndpoints ++
          allDIDRegistrarEndpoints ++
          allIssueEndpoints ++
          allPresentProofEndpoints ++
          allSystemEndpoints
      )
      server <- ZHttp4sBlazeServer.make
      appConfig <- ZIO.service[AppConfig]
      _ <- server.start(allEndpoints, port = appConfig.agent.httpEndpoint.http.port).debug
    } yield ()
}
