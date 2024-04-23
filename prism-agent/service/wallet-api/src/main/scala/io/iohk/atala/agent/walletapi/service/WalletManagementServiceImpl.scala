package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.event.notification.EventNotificationConfig
import io.iohk.atala.shared.crypto.Apollo
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletAdministrationContext
import io.iohk.atala.shared.models.WalletId
import zio.*

import java.util.UUID
import scala.language.implicitConversions

class WalletManagementServiceImpl(
    apollo: Apollo,
    nonSecretStorage: WalletNonSecretStorage,
    secretStorage: WalletSecretStorage,
) extends WalletManagementService {

  override def createWallet(
      wallet: Wallet,
      seed: Option[WalletSeed]
  ): ZIO[WalletAdministrationContext, WalletManagementServiceError, Wallet] =
    for {
      _ <- ZIO.serviceWithZIO[WalletAdministrationContext] {
        case WalletAdministrationContext.Admin()                                                   => ZIO.unit
        case WalletAdministrationContext.SelfService(permittedWallets) if permittedWallets.isEmpty => ZIO.unit
        case WalletAdministrationContext.SelfService(_) =>
          ZIO.fail(WalletManagementServiceError.TooManyPermittedWallet())
      }
      seed <- seed.fold(
        apollo.secp256k1.randomBip32Seed
          .flatMap { case (bytes, _) =>
            ZIO
              .fromEither(WalletSeed.fromByteArray(bytes))
              .orDieWith(msg => Exception(s"Wallet seed generation failed: $msg"))
          }
      )(ZIO.succeed)
      createdWallet <- nonSecretStorage
        .createWallet(wallet, seed.sha256Digest)
        .mapError[WalletManagementServiceError](e => e)
      _ <- secretStorage
        .setWalletSeed(seed)
        .mapError(WalletManagementServiceError.UnexpectedStorageError.apply)
        .provide(ZLayer.succeed(WalletAccessContext(wallet.id)))
    } yield createdWallet

  override def getWallet(
      walletId: WalletId
  ): ZIO[WalletAdministrationContext, WalletManagementServiceError, Option[Wallet]] = {
    ZIO
      .serviceWith[WalletAdministrationContext](_.isAuthorized(walletId))
      .flatMap {
        case true  => nonSecretStorage.getWallet(walletId).mapError(e => e)
        case false => ZIO.none
      }
  }

  override def getWallets(
      walletIds: Seq[WalletId]
  ): ZIO[WalletAdministrationContext, WalletManagementServiceError, Seq[Wallet]] = {
    ZIO
      .serviceWith[WalletAdministrationContext](ctx => walletIds.filter(ctx.isAuthorized))
      .flatMap { filteredIds => nonSecretStorage.getWallets(filteredIds).mapError(e => e) }
  }

  override def listWallets(
      offset: Option[Int],
      limit: Option[Int]
  ): ZIO[WalletAdministrationContext, WalletManagementServiceError, (Seq[Wallet], Int)] =
    ZIO.serviceWithZIO[WalletAdministrationContext] {
      case WalletAdministrationContext.Admin() =>
        nonSecretStorage
          .listWallet(offset = offset, limit = limit)
          .mapError(e => e)
      case WalletAdministrationContext.SelfService(permittedWallets) =>
        nonSecretStorage
          .getWallets(permittedWallets)
          .map(wallets => (wallets, wallets.length))
          .mapError(e => e)
    }

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
