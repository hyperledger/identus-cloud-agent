package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.memory.GenericSecretStorageInMemory
import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.service.WalletManagementServiceImpl
import io.iohk.atala.agent.walletapi.sql.JdbcGenericSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.agent.walletapi.vault.VaultGenericSecretStorage
import io.iohk.atala.agent.walletapi.vault.VaultWalletSecretStorage
import io.iohk.atala.shared.test.containers.PostgresTestContainerSupport
import io.iohk.atala.test.container.DBTestUtils
import io.iohk.atala.test.container.VaultTestContainerSupport
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.util.UUID
import zio.json.ast.Json
import io.iohk.atala.shared.models.WalletAccessContext

object GenericSecretStorageSpec
    extends ZIOSpecDefault,
      StorageSpecHelper,
      PostgresTestContainerSupport,
      VaultTestContainerSupport,
      ApolloSpecHelper {

  private def walletManagementServiceLayer =
    ZLayer.makeSome[WalletSecretStorage, WalletManagementService](
      WalletManagementServiceImpl.layer,
      JdbcWalletNonSecretStorage.layer,
      contextAwareTransactorLayer,
      apolloLayer
    )

  override def spec = {
    val jdbcTestSuite = commonSpec("JdbcGenericSecretStorage")
      .provide(
        JdbcWalletSecretStorage.layer,
        JdbcGenericSecretStorage.layer,
        contextAwareTransactorLayer,
        pgContainerLayer,
        walletManagementServiceLayer
      )

    val vaultTestSuite = commonSpec("VaultGenericSecretStorage")
      .provide(
        VaultWalletSecretStorage.layer,
        VaultGenericSecretStorage.layer,
        pgContainerLayer,
        vaultKvClientLayer,
        walletManagementServiceLayer
      )

    val inMemoryTestSuite = commonSpec("InMemoryGenericSecretStorage")
      .provide(
        JdbcWalletSecretStorage.layer,
        GenericSecretStorageInMemory.layer,
        contextAwareTransactorLayer,
        pgContainerLayer,
        walletManagementServiceLayer
      )

    suite("GenericSecretStorage")(jdbcTestSuite, vaultTestSuite, inMemoryTestSuite) @@ TestAspect.sequential
  }

  private def commonSpec(name: String) =
    suite(name)(singleWalletSpec, multiWalletSpec) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

  private val singleWalletSpec = suite("single-wallet")(
    test("insert and get the same item") {
      for {
        storage <- ZIO.service[GenericSecretStorage]
        id = UUID.randomUUID()
        secret = CredentialDefinitionSecret(json = Json.Obj("foo" -> Json.Str("bar")))
        _ <- storage.set(id, secret)
        result: Option[CredentialDefinitionSecret] <- storage.get(id)
      } yield assert(result)(isSome(equalTo(secret)))
    },
    test("insert item with same path return error") {
      for {
        storage <- ZIO.service[GenericSecretStorage]
        id = UUID.randomUUID()
        secret1 = CredentialDefinitionSecret(json = Json.Obj("foo1" -> Json.Str("bar1")))
        secret2 = CredentialDefinitionSecret(json = Json.Obj("foo2" -> Json.Str("bar2")))
        _ <- storage.set(id, secret1)
        exit <- storage.set(id, secret2).exit
      } yield assert(exit)(fails(anything))
    },
    test("get non-existing secret return none") {
      for {
        storage <- ZIO.service[GenericSecretStorage]
        id = UUID.randomUUID()
        result <- storage.get(id)
      } yield assert(result)(isNone)
    }
  ).globalWallet

  private val multiWalletSpec = suite("multi-wallet")(
    test("insert item with same path for different wallet do not fail") {
      for {
        walletSvc <- ZIO.service[WalletManagementService]
        walletId1 <- walletSvc.createWallet(Wallet("wallet-1")).map(_.id)
        walletId2 <- walletSvc.createWallet(Wallet("wallet-2")).map(_.id)
        storage <- ZIO.service[GenericSecretStorage]
        id = UUID.randomUUID()
        secret = CredentialDefinitionSecret(json = Json.Obj("foo" -> Json.Str("bar")))
        _ <- storage
          .set(id, secret)
          .provide(ZLayer.succeed(WalletAccessContext(walletId1)))
        _ <- storage
          .set(id, secret)
          .provide(ZLayer.succeed(WalletAccessContext(walletId2)))
        secret1 <- storage
          .get(id)
          .provide(ZLayer.succeed(WalletAccessContext(walletId1)))
        secret2 <- storage
          .get(id)
          .provide(ZLayer.succeed(WalletAccessContext(walletId2)))
      } yield assert(secret1)(equalTo(secret2)) && assert(secret1)(isSome)
    },
    test("do no see secret outside of the wallet") {
      for {
        walletSvc <- ZIO.service[WalletManagementService]
        walletId1 <- walletSvc.createWallet(Wallet("wallet-1")).map(_.id)
        walletId2 <- walletSvc.createWallet(Wallet("wallet-2")).map(_.id)
        storage <- ZIO.service[GenericSecretStorage]
        // wallet1 setup
        id1 = UUID.randomUUID()
        secret1 = CredentialDefinitionSecret(json = Json.Obj("foo1" -> Json.Str("bar1")))
        _ <- storage
          .set(id1, secret1)
          .provide(ZLayer.succeed(WalletAccessContext(walletId1)))
        // wallet2 setup
        id2 = UUID.randomUUID()
        secret2 = CredentialDefinitionSecret(json = Json.Obj("foo2" -> Json.Str("bar2")))
        _ <- storage
          .set(id2, secret2)
          .provide(ZLayer.succeed(WalletAccessContext(walletId2)))
        // assertions
        ownWallet1 <- storage
          .get(id1)
          .provide(ZLayer.succeed(WalletAccessContext(walletId1)))
        ownWallet2 <- storage
          .get(id2)
          .provide(ZLayer.succeed(WalletAccessContext(walletId2)))
        crossWallet1 <- storage
          .get(id1)
          .provide(ZLayer.succeed(WalletAccessContext(walletId2)))
        crossWallet2 <- storage
          .get(id2)
          .provide(ZLayer.succeed(WalletAccessContext(walletId1)))
      } yield assert(ownWallet1)(isSome(equalTo(secret1))) &&
        assert(ownWallet2)(isSome(equalTo(secret2))) &&
        assert(crossWallet1)(isNone) &&
        assert(crossWallet2)(isNone)
    }
  )

}
