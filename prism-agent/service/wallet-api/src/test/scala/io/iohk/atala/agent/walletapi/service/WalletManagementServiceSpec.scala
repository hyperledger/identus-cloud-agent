package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.agent.walletapi.vault.VaultWalletSecretStorage
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.test.container.DBTestUtils
import io.iohk.atala.test.container.PostgresTestContainerSupport
import io.iohk.atala.test.container.VaultTestContainerSupport
import zio.*
import zio.test.*
import zio.test.Assertion.*

object WalletManagementServiceSpec
    extends ZIOSpecDefault,
      PostgresTestContainerSupport,
      ApolloSpecHelper,
      VaultTestContainerSupport {

  override def spec = {
    def testSuite(name: String) =
      suite(name)(
        createWalletSpec
      ) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB) @@ TestAspect.sequential

    val suite1 = testSuite("jdbc as secret storage")
      .provide(
        WalletManagementServiceImpl.layer,
        JdbcWalletNonSecretStorage.layer,
        JdbcWalletSecretStorage.layer,
        transactorLayer,
        pgContainerLayer,
        apolloLayer
      )

    val suite2 = testSuite("vault as secret storage")
      .provide(
        WalletManagementServiceImpl.layer,
        JdbcWalletNonSecretStorage.layer,
        VaultWalletSecretStorage.layer,
        transactorLayer,
        pgContainerLayer,
        apolloLayer,
        vaultKvClientLayer
      )

    suite("WalletManagementService")(suite1, suite2)
  }

  private def createWalletSpec = suite("createWallet")(
    test("initialize with no wallet") {
      for {
        svc <- ZIO.service[WalletManagementService]
        walletIds <- svc.listWallets
      } yield assert(walletIds)(isEmpty)
    },
    test("create a wallet with random seed") {
      for {
        svc <- ZIO.service[WalletManagementService]
        secretStorage <- ZIO.service[WalletSecretStorage]
        createdWallet <- svc.createWallet()
        listedWallets <- svc.listWallets
        seed <- secretStorage.getWalletSeed.provide(ZLayer.succeed(WalletAccessContext(createdWallet)))
      } yield assert(listedWallets)(hasSameElements(Seq(createdWallet))) &&
        assert(seed)(isSome)
    },
    test("create multiple wallets with random seed") {
      for {
        svc <- ZIO.service[WalletManagementService]
        secretStorage <- ZIO.service[WalletSecretStorage]
        createdWallets <- ZIO
          .foreach(1 to 10) { _ =>
            svc.createWallet()
          }
        listedWallets <- svc.listWallets
        seeds <- ZIO.foreach(listedWallets) { walletId =>
          secretStorage.getWalletSeed.provide(ZLayer.succeed(WalletAccessContext(walletId)))
        }
      } yield assert(createdWallets)(hasSameElements(listedWallets)) &&
        assert(seeds)(forall(isSome))
    },
    test("create a wallet with provided seed") {
      for {
        svc <- ZIO.service[WalletManagementService]
        secretStorage <- ZIO.service[WalletSecretStorage]
        seed1 = WalletSeed.fromByteArray(Array.fill[Byte](64)(0))
        createdWallet <- svc.createWallet(Some(seed1))
        listedWallets <- svc.listWallets
        seed2 <- secretStorage.getWalletSeed.provide(ZLayer.succeed(WalletAccessContext(createdWallet)))
      } yield assert(listedWallets)(hasSameElements(Seq(createdWallet))) &&
        assert(seed2)(isSome(equalTo(seed1)))
    },
    test("create multiple wallets with provided seed") {
      for {
        svc <- ZIO.service[WalletManagementService]
        secretStorage <- ZIO.service[WalletSecretStorage]
        seeds1 = (1 to 10).map(i => WalletSeed.fromByteArray(Array.fill[Byte](64)(i.toByte)))
        createdWallets <- ZIO.foreach(seeds1) { seed => svc.createWallet(Some(seed)) }
        listedWallets <- svc.listWallets
        seeds2 <- ZIO.foreach(listedWallets) { walletId =>
          secretStorage.getWalletSeed.provide(ZLayer.succeed(WalletAccessContext(walletId)))
        }
      } yield assert(createdWallets)(hasSameElements(listedWallets)) &&
        assert(seeds2.flatten)(hasSameElements(seeds1))
    },
    test("create multiple wallets with same seed must not fail") {
      for {
        svc <- ZIO.service[WalletManagementService]
        seed = WalletSeed.fromByteArray(Array.fill[Byte](64)(0))
        _ <- svc.createWallet(Some(seed))
        _ <- svc.createWallet(Some(seed))
        _ <- svc.createWallet(Some(seed))
        wallets <- svc.listWallets
      } yield assert(wallets)(hasSize(equalTo(3))) &&
        assert(wallets)(isDistinct)
    }
  )

}
