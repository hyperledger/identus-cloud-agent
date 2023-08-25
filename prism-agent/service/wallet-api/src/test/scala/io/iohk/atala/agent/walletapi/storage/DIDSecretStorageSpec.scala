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
    val jdbcTestSuite =
      commonSpec("JdbcDIDSecretStorage")
        .provide(
          JdbcDIDNonSecretStorage.layer,
          JdbcDIDSecretStorage.layer,
          JdbcWalletSecretStorage.layer,
          contextAwareTransactorLayer,
          pgContainerLayer,
          walletManagementServiceLayer
        )

    val vaultTestSuite = commonSpec("VaultDIDSecretStorage")
      .provide(
        JdbcDIDNonSecretStorage.layer,
        VaultDIDSecretStorage.layer,
        VaultWalletSecretStorage.layer,
        contextAwareTransactorLayer,
        pgContainerLayer,
        vaultKvClientLayer,
        walletManagementServiceLayer
      )

    suite("DIDSecretStorage")(jdbcTestSuite, vaultTestSuite) @@ TestAspect.sequential
  }

  private def commonSpec(name: String) = suite(name)(
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
  ).globalWallet @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

}
