package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

class WalletManagementServiceImpl(
    apollo: Apollo,
    nonSecretStorage: WalletNonSecretStorage,
    secretStorage: WalletSecretStorage,
) extends WalletManagementService {

  override def createWallet(seed: Option[WalletSeed]): IO[WalletManagementServiceError, WalletId] =
    for {
      seed <- seed.fold(
        apollo.ecKeyFactory
          .randomBip32Seed()
          .map(_._1)
          .flatMap(bytes => ZIO.fromEither(WalletSeed.fromByteArray(bytes)).mapError(Exception(_)))
          .mapError(WalletManagementServiceError.SeedGenerationError.apply)
      )(ZIO.succeed)
      walletId <- nonSecretStorage.createWallet
        .mapError(WalletManagementServiceError.WalletStorageError.apply)
      _ <- secretStorage
        .setWalletSeed(seed)
        .mapError(WalletManagementServiceError.WalletStorageError.apply)
        .provide(ZLayer.succeed(WalletAccessContext(walletId)))
    } yield walletId

  override def listWallets(
      offset: Option[Int],
      limit: Option[Int]
  ): IO[WalletManagementServiceError, (Seq[WalletId], Int)] =
    nonSecretStorage
      .listWallet(offset = offset, limit = limit)
      .mapError(WalletManagementServiceError.WalletStorageError.apply)
}

object WalletManagementServiceImpl {
  val layer: URLayer[Apollo & WalletNonSecretStorage & WalletSecretStorage, WalletManagementService] = {
    ZLayer.fromFunction(WalletManagementServiceImpl(_, _, _))
  }
}
