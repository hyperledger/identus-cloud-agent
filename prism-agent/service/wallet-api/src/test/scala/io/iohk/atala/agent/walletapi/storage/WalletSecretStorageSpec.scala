package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.agent.walletapi.vault.VaultWalletSecretStorage
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.test.container.DBTestUtils
import io.iohk.atala.test.container.PostgresTestContainerSupport
import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.test.container.VaultTestContainerSupport

object WalletSecretStorageSpec extends ZIOSpecDefault, PostgresTestContainerSupport, VaultTestContainerSupport {

  override def spec = {
    def testSuite(name: String) = suite(name)(
      setWalletSeedSpec
    ) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

    val suite1 = testSuite("jdbc as storage")
      .provide(
        JdbcWalletSecretStorage.layer,
        JdbcWalletNonSecretStorage.layer,
        transactorLayer,
        pgContainerLayer
      )

    val suite2 = testSuite("jdbc as storage")
      .provide(
        VaultWalletSecretStorage.layer,
        JdbcWalletNonSecretStorage.layer,
        transactorLayer,
        pgContainerLayer,
        vaultKvClientLayer
      )

    suite("WalletSecretStorage")(suite1, suite2)
  }

  private def setWalletSeedSpec = suite("setWalletSeed")(
    test("set seed on a new wallet") {
      for {
        storage <- ZIO.service[WalletSecretStorage]
        walletId <- ZIO.serviceWithZIO[WalletNonSecretStorage](_.createWallet)
        walletAccessCtx = ZLayer.succeed(WalletAccessContext(walletId))
        seed = WalletSeed.fromByteArray(Array.fill[Byte](64)(0))
        seedBefore <- storage.getWalletSeed.provide(walletAccessCtx)
        _ <- storage.setWalletSeed(seed).provide(walletAccessCtx)
        seedAfter <- storage.getWalletSeed.provide(walletAccessCtx)
      } yield assert(seedBefore)(isNone) &&
        assert(seedAfter)(isSome(equalTo(seed)))
    },
    test("set seed on multiple wallets") {
      for {
        storage <- ZIO.service[WalletSecretStorage]
        walletIds <- ZIO.foreach(1 to 10) { i =>
          for {
            walletId <- ZIO.serviceWithZIO[WalletNonSecretStorage](_.createWallet)
            seed = WalletSeed.fromByteArray(Array.fill[Byte](64)(i.toByte))
            walletAccessCtx = ZLayer.succeed(WalletAccessContext(walletId))
            _ <- storage.setWalletSeed(seed).provideSomeLayer(walletAccessCtx)
          } yield walletId
        }
        seeds <- ZIO
          .foreach(walletIds) { walletId =>
            val walletAccessCtx = ZLayer.succeed(WalletAccessContext(walletId))
            storage.getWalletSeed.provideSomeLayer(walletAccessCtx)
          }
          .map(_.flatten)
      } yield assert(seeds.size)(equalTo(10)) && assert(seeds)(isDistinct)
    }
  )

}
