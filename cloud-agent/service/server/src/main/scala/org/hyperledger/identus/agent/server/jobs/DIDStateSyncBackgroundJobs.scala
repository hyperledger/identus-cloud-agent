package org.hyperledger.identus.agent.server.jobs

import org.hyperledger.identus.agent.server.config.AppConfig
import org.hyperledger.identus.agent.walletapi.service.{ManagedDIDService, WalletManagementService}
import org.hyperledger.identus.shared.messaging.{Message, MessagingService, Producer}
import org.hyperledger.identus.shared.models.{WalletAccessContext, WalletAdministrationContext, WalletId}
import org.hyperledger.identus.shared.utils.DurationOps.toMetricsSeconds
import zio.*
import zio.metrics.Metric

object DIDStateSyncBackgroundJobs extends BackgroundJobsHelper {

  private val TOPIC_NAME = "sync-did-state"

  val didPublicationStateSyncTrigger
      : URIO[ManagedDIDService & WalletManagementService & Producer[WalletId, WalletId], Unit] =
    ZIO
      .serviceWithZIO[WalletManagementService](_.listWallets().map(_._1))
      .flatMap { wallets =>
        ZIO.foreach(wallets) { wallet =>
          for {
            producer <- ZIO.service[Producer[WalletId, WalletId]]
            _ <- producer.produce(TOPIC_NAME, wallet.id, wallet.id)
          } yield ()
        }
      }
      .catchAll(e => ZIO.logError(s"error while syncing DID publication state: $e"))
      .repeat(Schedule.spaced(10.seconds))
      .provideSomeLayer(ZLayer.succeed(WalletAdministrationContext.Admin()))
      .debug
      .fork
      .unit

  val didPublicationStateSyncHandler = for {
    appConfig <- ZIO.service[AppConfig]
    _ <- MessagingService.consumeWithRetryStrategy(
      "identus-cloud-agent",
      DIDStateSyncBackgroundJobs.handleMessage,
      retryStepsFromConfig(TOPIC_NAME, appConfig.agent.kafka.consumers.didStateSync)
    )
  } yield ()

  private def handleMessage(message: Message[WalletId, WalletId]): RIO[ManagedDIDService, Unit] = {
    val effect = for {
      managedDidService <- ZIO.service[ManagedDIDService]
      _ <- managedDidService.syncManagedDIDState
      _ <- managedDidService.syncUnconfirmedUpdateOperations
    } yield ()
    effect
      .provideSomeLayer(ZLayer.succeed(WalletAccessContext(message.value)))
      .catchAll(t => ZIO.logErrorCause("Unable to syncing DID publication state", Cause.fail(t)))
      @@ Metric
        .gauge("did_publication_state_sync_job_ms_gauge")
        .trackDurationWith(_.toMetricsSeconds)
  }
}
