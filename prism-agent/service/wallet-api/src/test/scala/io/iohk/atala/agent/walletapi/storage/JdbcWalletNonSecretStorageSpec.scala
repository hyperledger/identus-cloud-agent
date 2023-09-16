package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorageError.TooManyWebhook
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorageError.DuplicatedWalletId
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorageError.DuplicatedWalletSeed
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

  private def createWallets(n: Int) =
    ZIO.foreach(1 to n) { i =>
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        seedDigest <- Random.nextBytes(32).map(_.toArray)
        wallet <- storage.createWallet(Wallet(s"wallet-$i"), seedDigest)
      } yield wallet
    }

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
        wallet <- createWallets(1).map(_.head)
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
        _ <- createWallets(1)
        _ <- createWallets(1)
        wallets <- storage.listWallet().map(_._1)
        names = wallets.map(_.name)
      } yield assert(names)(hasSameElements(Seq("wallet-1", "wallet-1")))
    },
    test("create wallet with same id fail with duplicate id error") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        seedDigest1 <- Random.nextBytes(32).map(_.toArray)
        seedDigest2 <- Random.nextBytes(32).map(_.toArray)
        _ <- storage.createWallet(Wallet("wallet-1", WalletId.default), seedDigest1)
        exit <- storage.createWallet(Wallet("wallet-2", WalletId.default), seedDigest2).exit
      } yield assert(exit)(fails(isSubtype[DuplicatedWalletId](anything)))
    },
    test("create wallet with same seed digest fail with duplicate seed error") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        seedDigest1 <- Random.nextBytes(32).map(_.toArray)
        _ <- storage.createWallet(Wallet("wallet-1", WalletId.random), seedDigest1)
        exit <- storage.createWallet(Wallet("wallet-2", WalletId.random), seedDigest1).exit
      } yield assert(exit)(fails(isSubtype[DuplicatedWalletSeed](anything)))
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
        _ <- createWallets(3)
        walletsWithCount <- storage.listWallet()
        (wallets, count) = walletsWithCount
      } yield assert(wallets.map(_.name))(hasSameElements(Seq("wallet-1", "wallet-2", "wallet-3"))) &&
        assert(count)(equalTo(3))
    },
    test("list stored wallet and return correct item count when using offset and limit") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- createWallets(50)
        walletsWithCount <- storage.listWallet(offset = Some(20), limit = Some(20))
        (pagedWallets, count) = walletsWithCount
      } yield assert(wallets.drop(20).take(20))(equalTo(pagedWallets)) &&
        assert(count)(equalTo(50))
    },
    test("list stored wallet and return correct item count when using offset only") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- createWallets(50)
        walletsWithCount <- storage.listWallet(offset = Some(20))
        (pagedWallets, count) = walletsWithCount
      } yield assert(wallets.drop(20))(equalTo(pagedWallets)) &&
        assert(count)(equalTo(50))
    },
    test("list stored wallet and return correct item count when using limit only") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- createWallets(50)
        walletsWithCount <- storage.listWallet(limit = Some(20))
        (pagedWallets, count) = walletsWithCount
      } yield assert(wallets.take(20))(equalTo(pagedWallets)) &&
        assert(count)(equalTo(50))
    },
    test("return empty list when limit is zero") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- createWallets(50)
        walletsWithCount <- storage.listWallet(limit = Some(0))
        (pagedWallets, count) = walletsWithCount
      } yield assert(pagedWallets)(isEmpty) && assert(count)(equalTo(50))
    },
    test("fail when limit is negative") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- createWallets(50)
        exit <- storage.listWallet(limit = Some(-1)).exit
      } yield assert(exit)(fails(anything))
    },
    test("fail when offset is negative") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallets <- createWallets(50)
        exit <- storage.listWallet(offset = Some(-1)).exit
      } yield assert(exit)(fails(anything))
    }
  )

  private val walletNotificationSpec = suite("walletNotification")(
    test("insert wallet notification") {
      for {
        storage <- ZIO.service[WalletNonSecretStorage]
        wallet <- createWallets(1).map(_.head)
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
        wallet <- createWallets(1).map(_.head)
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
        wallets <- createWallets(2)
        wallet1 = wallets.head
        wallet2 = wallets.last
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
