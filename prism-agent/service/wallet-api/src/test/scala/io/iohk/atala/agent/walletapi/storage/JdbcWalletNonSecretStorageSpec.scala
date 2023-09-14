package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorageRefinedError.TooManyWebhook
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorageRefinedError.DuplicatedWalletId
import io.iohk.atala.event.notification.EventNotificationConfig
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import io.iohk.atala.shared.test.containers.PostgresTestContainerSupport
import io.iohk.atala.test.container.DBTestUtils
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.net.URL

object JdbcWalletNonSecretStorageSpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  override def spec = {
    val s = suite("JdbcWalletNonSecretStorage")(
      getWalletSpec,
      listWalletSpec,
      createWalletSpec,
      walletNotificationSpec
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

  private val createWalletSpec = suite("createWallet")(
    test("create wallet with same name should not fail") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        _ <- storage.createWallet(Wallet("wallet-1"))
        _ <- storage.createWallet(Wallet("wallet-1"))
        wallets <- storage.listWallet().map(_._1).debug("wallets")
        names = wallets.map(_.name)
      } yield assert(names)(hasSameElements(Seq("wallet-1", "wallet-1")))
    },
    test("create wallet with same id fail with duplicate id error") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        _ <- storage.createWallet(Wallet("wallet-1", WalletId.default))
        exit <- storage.createWallet(Wallet("wallet-2", WalletId.default)).exit
      } yield assert(exit)(fails(isSubtype[DuplicatedWalletId](anything)))
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

  private val walletNotificationSpec = suite("walletNotification")(
    test("insert wallet notification") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallet <- storage.createWallet(Wallet("wallet-1"))
        config = EventNotificationConfig(wallet.id, URL("https://example.com"))
        _ <- storage
          .createWalletNotification(config)
          .provide(ZLayer.succeed(WalletAccessContext(wallet.id)))
        configs <- storage.walletNotification
          .provide(ZLayer.succeed(WalletAccessContext(wallet.id)))
      } yield assert(configs)(hasSameElements(Seq(config)))
    },
    test("unable to create new notification if exceed limit")(
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallet <- storage.createWallet(Wallet("wallet-1"))
        limit = JdbcWalletNonSecretStorage.MAX_WEBHOOK_PER_WALLET
        _ <- ZIO.foreach(1 to limit) { _ =>
          storage
            .createWalletNotification(EventNotificationConfig(wallet.id, URL("https://example.com")))
            .provide(ZLayer.succeed(WalletAccessContext(wallet.id)))
        }
        exit <- storage
          .createWalletNotification(EventNotificationConfig(wallet.id, URL("https://example.com")))
          .provide(ZLayer.succeed(WalletAccessContext(wallet.id)))
          .exit
      } yield assert(exit)(fails(isSubtype[TooManyWebhook](anything)))
    ),
    test("do not see notification outside of the wallet") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallet1 <- storage.createWallet(Wallet("wallet-1"))
        wallet2 <- storage.createWallet(Wallet("wallet-2"))
        config = EventNotificationConfig(wallet1.id, URL("https://example.com"))
        _ <- storage
          .createWalletNotification(config)
          .provide(ZLayer.succeed(WalletAccessContext(wallet1.id)))
        notifications1 <- storage.walletNotification.provide(ZLayer.succeed(WalletAccessContext(wallet1.id)))
        notifications2 <- storage.walletNotification.provide(ZLayer.succeed(WalletAccessContext(wallet2.id)))
      } yield assert(notifications1)(hasSameElements(Seq(config))) && assert(notifications2)(isEmpty)
    }
  )
}
