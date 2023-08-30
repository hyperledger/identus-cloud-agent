package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.shared.test.containers.PostgresTestContainerSupport
import io.iohk.atala.test.container.DBTestUtils
import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.shared.models.WalletId

object JdbcWalletNonSecretStorageSpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  override def spec = {
    val s = suite("JdbcWalletNonSecretStorage")(
      getWalletSpec,
      listWalletSpec,
    ) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

    s.provide(JdbcWalletNonSecretStorage.layer, contextAwareTransactorLayer, pgContainerLayer)
  }

  private val getWalletSpec = suite("getWallet")(
    test("get existing wallet") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallet <- storage.createWallet(Wallet("wallet-1"))
        wallet2 <- storage.getWallet(wallet.id)
      } yield assert(wallet2)(isSome(equalTo(wallet)))
    },
    test("get non-existing wallet") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallet <- storage.getWallet(WalletId.random)
      } yield assert(wallet)(isNone)
    }
  )

  private val listWalletSpec = suite("listWallet")(
    test("initialize with empty wallet") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        walletsWithCount <- storage.listWallet()
        (wallets, count) = walletsWithCount
      } yield assert(wallets)(isEmpty) && assert(count)(isZero)
    },
    test("list created wallets") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        _ <- storage.createWallet(Wallet("wallet-1"))
        _ <- storage.createWallet(Wallet("wallet-2"))
        _ <- storage.createWallet(Wallet("wallet-3"))
        walletsWithCount <- storage.listWallet()
        (wallets, count) = walletsWithCount
      } yield assert(wallets.map(_.name))(hasSameElements(Seq("wallet-1", "wallet-2", "wallet-3"))) &&
        assert(count)(equalTo(3))
    },
    test("list stored wallet and return correct item count when using offset and limit") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- ZIO.foreach(1 to 50) { i => storage.createWallet(Wallet(s"wallet-$i")) }
        walletsWithCount <- storage.listWallet(offset = Some(20), limit = Some(20))
        (pagedWallets, count) = walletsWithCount
      } yield assert(wallets.drop(20).take(20))(equalTo(pagedWallets)) &&
        assert(count)(equalTo(50))
    },
    test("list stored wallet and return correct item count when using offset only") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- ZIO.foreach(1 to 50) { i => storage.createWallet(Wallet(s"wallet-$i")) }
        walletsWithCount <- storage.listWallet(offset = Some(20))
        (pagedWallets, count) = walletsWithCount
      } yield assert(wallets.drop(20))(equalTo(pagedWallets)) &&
        assert(count)(equalTo(50))
    },
    test("list stored wallet and return correct item count when using limit only") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- ZIO.foreach(1 to 50) { i => storage.createWallet(Wallet(s"wallet-$i")) }
        walletsWithCount <- storage.listWallet(limit = Some(20))
        (pagedWallets, count) = walletsWithCount
      } yield assert(wallets.take(20))(equalTo(pagedWallets)) &&
        assert(count)(equalTo(50))
    },
    test("return empty list when limit is zero") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- ZIO.foreach(1 to 50) { i => storage.createWallet(Wallet(s"wallet-$i")) }
        walletsWithCount <- storage.listWallet(limit = Some(0))
        (pagedWallets, count) = walletsWithCount
      } yield assert(pagedWallets)(isEmpty) && assert(count)(equalTo(50))
    },
    test("fail when limit is negative") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- ZIO.foreach(1 to 50) { i => storage.createWallet(Wallet(s"wallet-$i")) }
        exit <- storage.listWallet(limit = Some(-1)).exit
      } yield assert(exit)(fails(anything))
    },
    test("fail when offset is negative") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- ZIO.foreach(1 to 50) { i => storage.createWallet(Wallet(s"wallet-$i")) }
        exit <- storage.listWallet(offset = Some(-1)).exit
      } yield assert(exit)(fails(anything))
    }
  )

}
