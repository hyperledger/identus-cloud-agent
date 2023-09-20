package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.service.{WalletManagementService, WalletManagementServiceImpl}
import io.iohk.atala.agent.walletapi.sql.{
  JdbcDIDNonSecretStorage,
  JdbcDIDSecretStorage,
  JdbcWalletNonSecretStorage,
  JdbcWalletSecretStorage
}
import io.iohk.atala.agent.walletapi.vault.{VaultDIDSecretStorage, VaultWalletSecretStorage}
import io.iohk.atala.mercury.PeerDID
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.test.containers.PostgresTestContainerSupport
import io.iohk.atala.test.container.{DBTestUtils, VaultTestContainerSupport}
import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.agent.walletapi.memory.DIDSecretStorageInMemory
import io.iohk.atala.agent.walletapi.memory.WalletSecretStorageInMemory
import io.iohk.atala.agent.walletapi.model.Wallet

object DIDSecretStorageSpec
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

  override def spec: Spec[TestEnvironment & Scope, Any] = {
    val jdbcTestSuite = commonSpec("JdbcDIDSecretStorage")
      .provide(
        JdbcDIDNonSecretStorage.layer,
        JdbcDIDSecretStorage.layer,
        JdbcWalletSecretStorage.layer,
        systemTransactorLayer,
        contextAwareTransactorLayer,
        pgContainerLayer,
        walletManagementServiceLayer
      )

    val vaultTestSuite = commonSpec("VaultDIDSecretStorage")
      .provide(
        JdbcDIDNonSecretStorage.layer,
        VaultDIDSecretStorage.layer,
        VaultWalletSecretStorage.layer,
        systemTransactorLayer,
        contextAwareTransactorLayer,
        pgContainerLayer,
        vaultKvClientLayer,
        walletManagementServiceLayer
      )

    val inMemoryTestSuite = commonSpec("InMemoryDIDSecretStorage")
      .provide(
        JdbcDIDNonSecretStorage.layer,
        DIDSecretStorageInMemory.layer,
        WalletSecretStorageInMemory.layer,
        systemTransactorLayer,
        contextAwareTransactorLayer,
        pgContainerLayer,
        walletManagementServiceLayer
      )

    suite("DIDSecretStorage")(jdbcTestSuite, vaultTestSuite, inMemoryTestSuite) @@ TestAspect.sequential
  }

  private def commonSpec(name: String) =
    suite(name)(singleWalletSpec, multiWalletSpec) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

  private val singleWalletSpec = suite("single-wallet")(
    test("insert and get the same key for OctetKeyPair") {
      for {
        nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
        secretStorage <- ZIO.service[DIDSecretStorage]
        peerDID = PeerDID.makePeerDid()
        _ <- nonSecretStorage.createPeerDIDRecord(peerDID.did)
        n1 <- secretStorage.insertKey(peerDID.did, "agreement", peerDID.jwkForKeyAgreement)
        n2 <- secretStorage.insertKey(peerDID.did, "authentication", peerDID.jwkForKeyAuthentication)
        key1 <- secretStorage.getKey(peerDID.did, "agreement")
        key2 <- secretStorage.getKey(peerDID.did, "authentication")
      } yield assert(n1)(equalTo(1)) &&
        assert(n2)(equalTo(1)) &&
        assert(key1)(isSome(equalTo(peerDID.jwkForKeyAgreement))) &&
        assert(key2)(isSome(equalTo(peerDID.jwkForKeyAuthentication)))
    },
    test("insert same key id return error") {
      for {
        nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
        secretStorage <- ZIO.service[DIDSecretStorage]
        peerDID = PeerDID.makePeerDid()
        _ <- nonSecretStorage.createPeerDIDRecord(peerDID.did)
        n1 <- secretStorage.insertKey(peerDID.did, "agreement", peerDID.jwkForKeyAgreement)
        exit <- secretStorage
          .insertKey(peerDID.did, "agreement", peerDID.jwkForKeyAuthentication)
          .exit
        key1 <- secretStorage.getKey(peerDID.did, "agreement")
      } yield assert(n1)(equalTo(1)) &&
        assert(exit)(fails(anything)) &&
        assert(key1)(isSome(equalTo(peerDID.jwkForKeyAgreement)))
    },
    test("get non-exist key return none") {
      for {
        secretStorage <- ZIO.service[DIDSecretStorage]
        peerDID = PeerDID.makePeerDid()
        key1 <- secretStorage.getKey(peerDID.did, "agreement")
      } yield assert(key1)(isNone)
    },
  ).globalWallet

  private val multiWalletSpec = suite("multi-wallet")(
    test("do not see peer DID key outside of the wallet") {
      for {
        walletSvc <- ZIO.service[WalletManagementService]
        walletId1 <- walletSvc.createWallet(Wallet("wallet-1")).map(_.id)
        walletId2 <- walletSvc.createWallet(Wallet("wallet-2")).map(_.id)
        nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
        secretStorage <- ZIO.service[DIDSecretStorage]
        // wallet1 setup
        peerDID1 = PeerDID.makePeerDid()
        _ <- nonSecretStorage
          .createPeerDIDRecord(peerDID1.did)
          .provide(ZLayer.succeed(WalletAccessContext(walletId1)))
        _ <- secretStorage
          .insertKey(peerDID1.did, "key-1", peerDID1.jwkForKeyAgreement)
          .provide(ZLayer.succeed(WalletAccessContext(walletId1)))
        // wallet2 setup
        peerDID2 = PeerDID.makePeerDid()
        _ <- nonSecretStorage
          .createPeerDIDRecord(peerDID2.did)
          .provide(ZLayer.succeed(WalletAccessContext(walletId2)))
        _ <- secretStorage
          .insertKey(peerDID2.did, "key-1", peerDID2.jwkForKeyAgreement)
          .provide(ZLayer.succeed(WalletAccessContext(walletId2)))
        // assertions
        ownWallet1 <- secretStorage
          .getKey(peerDID1.did, "key-1")
          .provide(ZLayer.succeed(WalletAccessContext(walletId1)))
        ownWallet2 <- secretStorage
          .getKey(peerDID2.did, "key-1")
          .provide(ZLayer.succeed(WalletAccessContext(walletId2)))
        crossWallet1 <- secretStorage
          .getKey(peerDID1.did, "key-1")
          .provide(ZLayer.succeed(WalletAccessContext(walletId2)))
        crossWallet2 <- secretStorage
          .getKey(peerDID2.did, "key-1")
          .provide(ZLayer.succeed(WalletAccessContext(walletId1)))
      } yield assert(ownWallet1)(isSome(equalTo(peerDID1.jwkForKeyAgreement))) &&
        assert(ownWallet2)(isSome(equalTo(peerDID2.jwkForKeyAgreement))) &&
        assert(crossWallet1)(isNone) &&
        assert(crossWallet2)(isNone)
    }
  )

}
