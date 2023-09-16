package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.service.WalletManagementServiceError.DuplicatedWalletSeed
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.agent.walletapi.vault.VaultWalletSecretStorage
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import io.iohk.atala.shared.test.containers.PostgresTestContainerSupport
import io.iohk.atala.test.container.DBTestUtils
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
        createWalletSpec,
        getWalletSpec,
      ) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB) @@ TestAspect.sequential

    val suite1 = testSuite("jdbc as secret storage")
      .provide(
        WalletManagementServiceImpl.layer,
        JdbcWalletNonSecretStorage.layer,
        JdbcWalletSecretStorage.layer,
        contextAwareTransactorLayer,
        pgContainerLayer,
        apolloLayer
      )

    val suite2 = testSuite("vault as secret storage")
      .provide(
        WalletManagementServiceImpl.layer,
        JdbcWalletNonSecretStorage.layer,
        VaultWalletSecretStorage.layer,
        contextAwareTransactorLayer,
        pgContainerLayer,
        apolloLayer,
        vaultKvClientLayer
      )

    suite("WalletManagementService")(suite1, suite2)
  }

  private def getWalletSpec = suite("getWallet")(
    test("get existing wallet") {
      for {
        svc <- ZIO.service[WalletManagementService]
        createdWallet <- svc.createWallet(Wallet("wallet-1"))
        wallet <- svc.getWallet(createdWallet.id)
      } yield assert(wallet)(isSome(equalTo(createdWallet)))
    },
    test("get non-existing wallet") {
      for {
        svc <- ZIO.service[WalletManagementService]
        wallet <- svc.getWallet(WalletId.random)
      } yield assert(wallet)(isNone)
    },
  )

  private def createWalletSpec = suite("createWallet")(
    test("initialize with no wallet") {
      for {
        svc <- ZIO.service[WalletManagementService]
        walletIds <- svc.listWallets().map(_._1)
      } yield assert(walletIds)(isEmpty)
    },
    test("create a wallet with random seed") {
      for {
        svc <- ZIO.service[WalletManagementService]
        secretStorage <- ZIO.service[WalletSecretStorage]
        createdWallet <- svc.createWallet(Wallet("wallet-1"))
        listedWallets <- svc.listWallets().map(_._1)
        seed <- secretStorage.getWalletSeed.provide(ZLayer.succeed(WalletAccessContext(createdWallet.id)))
      } yield assert(listedWallets)(hasSameElements(Seq(createdWallet))) &&
        assert(seed)(isSome)
    },
    test("create multiple wallets with random seed") {
      for {
        svc <- ZIO.service[WalletManagementService]
        secretStorage <- ZIO.service[WalletSecretStorage]
        createdWallets <- ZIO.foreach(1 to 10)(i => svc.createWallet(Wallet(s"wallet-$i")))
        listedWallets <- svc.listWallets().map(_._1)
        seeds <- ZIO.foreach(listedWallets) { wallet =>
          secretStorage.getWalletSeed.provide(ZLayer.succeed(WalletAccessContext(wallet.id)))
        }
      } yield assert(createdWallets)(hasSameElements(listedWallets)) &&
        assert(seeds)(forall(isSome))
    },
    test("create a wallet with provided seed") {
      for {
        svc <- ZIO.service[WalletManagementService]
        secretStorage <- ZIO.service[WalletSecretStorage]
        seed1 = WalletSeed.fromByteArray(Array.fill[Byte](64)(0)).toOption.get
        createdWallet <- svc.createWallet(Wallet("wallet-1"), Some(seed1))
        listedWallets <- svc.listWallets().map(_._1)
        seed2 <- secretStorage.getWalletSeed.provide(ZLayer.succeed(WalletAccessContext(createdWallet.id)))
      } yield assert(listedWallets)(hasSameElements(Seq(createdWallet))) &&
        assert(seed2)(isSome(equalTo(seed1)))
    },
    test("create multiple wallets with provided seed") {
      for {
        svc <- ZIO.service[WalletManagementService]
        secretStorage <- ZIO.service[WalletSecretStorage]
        seeds1 = (1 to 10).map(i => WalletSeed.fromByteArray(Array.fill[Byte](64)(i.toByte)).toOption.get)
        createdWallets <- ZIO.foreach(seeds1) { seed => svc.createWallet(Wallet("test-wallet"), Some(seed)) }
        listedWallets <- svc.listWallets().map(_._1)
        seeds2 <- ZIO.foreach(listedWallets) { wallet =>
          secretStorage.getWalletSeed.provide(ZLayer.succeed(WalletAccessContext(wallet.id)))
        }
      } yield assert(createdWallets)(hasSameElements(listedWallets)) &&
        assert(seeds2.flatten)(hasSameElements(seeds1))
    },
    test("create multiple wallets with same seed must fail") {
      for {
        svc <- ZIO.service[WalletManagementService]
        seed = WalletSeed.fromByteArray(Array.fill[Byte](64)(0)).toOption.get
        _ <- svc.createWallet(Wallet("wallet-1"), Some(seed))
        exit <- svc.createWallet(Wallet("wallet-2"), Some(seed)).exit
      } yield assert(exit)(fails(isSubtype[DuplicatedWalletSeed](anything)))
    }
  )

}
