package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.event.notification.EventNotificationConfig
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

import java.util.UUID
import scala.language.implicitConversions

class WalletManagementServiceImpl(
    apollo: Apollo,
    nonSecretStorage: WalletNonSecretStorage,
    secretStorage: WalletSecretStorage,
) extends WalletManagementService {

  override def createWallet(wallet: Wallet, seed: Option[WalletSeed]): IO[WalletManagementServiceError, Wallet] =
    for {
      seed <- seed.fold(
        apollo.ecKeyFactory
          .randomBip32Seed()
          .map(_._1)
          .flatMap(bytes => ZIO.fromEither(WalletSeed.fromByteArray(bytes)).mapError(Exception(_)))
          .mapError(WalletManagementServiceError.SeedGenerationError.apply)
      )(ZIO.succeed)
      createdWallet <- nonSecretStorage
        .createWallet(wallet, seed.sha256Digest)
        .mapError[WalletManagementServiceError](e => e)
      _ <- secretStorage
        .setWalletSeed(seed)
        .mapError(WalletManagementServiceError.UnexpectedStorageError.apply)
        .provide(ZLayer.succeed(WalletAccessContext(wallet.id)))
    } yield createdWallet

  override def getWallet(walletId: WalletId): IO[WalletManagementServiceError, Option[Wallet]] =
    nonSecretStorage
      .getWallet(walletId)
      .mapError(e => e)

  override def listWallets(
      offset: Option[Int],
      limit: Option[Int]
  ): IO[WalletManagementServiceError, (Seq[Wallet], Int)] =
    nonSecretStorage
      .listWallet(offset = offset, limit = limit)
      .mapError(e => e)

  override def listWalletNotifications
      : ZIO[WalletAccessContext, WalletManagementServiceError, Seq[EventNotificationConfig]] =
    nonSecretStorage.walletNotification
      .mapError(e => e)

  override def createWalletNotification(
      config: EventNotificationConfig
  ): ZIO[WalletAccessContext, WalletManagementServiceError, EventNotificationConfig] =
    nonSecretStorage
      .createWalletNotification(config)
      .mapError(e => e)

  override def deleteWalletNotification(id: UUID): ZIO[WalletAccessContext, WalletManagementServiceError, Unit] =
    nonSecretStorage
      .deleteWalletNotification(id)
      .mapError(e => e)

}

object WalletManagementServiceImpl {
  val layer: URLayer[Apollo & WalletNonSecretStorage & WalletSecretStorage, WalletManagementService] = {
    ZLayer.fromFunction(WalletManagementServiceImpl(_, _, _))
  }
}
