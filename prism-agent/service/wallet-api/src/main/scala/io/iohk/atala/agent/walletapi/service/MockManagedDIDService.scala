package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.*
import io.iohk.atala.agent.walletapi.model.error.*
import io.iohk.atala.agent.walletapi.storage.DIDNonSecretStorage
import io.iohk.atala.castor.core.model.did.{
  CanonicalPrismDID,
  LongFormPrismDID,
  PrismDIDOperation,
  ScheduleDIDOperationOutcome
}
import io.iohk.atala.mercury.PeerDID
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.crypto.Ed25519KeyPair
import io.iohk.atala.shared.crypto.Secp256k1KeyPair
import io.iohk.atala.shared.crypto.X25519KeyPair
import zio.mock.*
import zio.test.Assertion
import zio.{mock, *}

import java.security.{PrivateKey as JavaPrivateKey, PublicKey as JavaPublicKey}

object MockManagedDIDService extends Mock[ManagedDIDService] {

  object GetManagedDIDState extends Effect[CanonicalPrismDID, GetManagedDIDError, Option[ManagedDIDState]]
  object JavaKeyPairWithDID
      extends Effect[(CanonicalPrismDID, String), GetKeyError, Option[(JavaPrivateKey, JavaPublicKey)]]

  override val compose: URLayer[mock.Proxy, ManagedDIDService] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new ManagedDIDService {
        override def nonSecretStorage: DIDNonSecretStorage = ???

        override def syncManagedDIDState: IO[GetManagedDIDError, Unit] = ???

        override def syncUnconfirmedUpdateOperations: IO[GetManagedDIDError, Unit] = ???

        override def javaKeyPairWithDID(
            did: CanonicalPrismDID,
            keyId: String
        ): IO[GetKeyError, Option[(JavaPrivateKey, JavaPublicKey)]] =
          proxy(JavaKeyPairWithDID, did, keyId)

        override def findDIDKeyPair(
            did: CanonicalPrismDID,
            keyId: String
        ): IO[GetKeyError, Option[Secp256k1KeyPair | Ed25519KeyPair | X25519KeyPair]] = ???

        override def getManagedDIDState(
            did: CanonicalPrismDID
        ): IO[GetManagedDIDError, Option[ManagedDIDState]] =
          proxy(GetManagedDIDState, did)

        override def listManagedDIDPage(
            offset: Int,
            limit: Int
        ): IO[GetManagedDIDError, (Seq[ManagedDIDDetail], Int)] = ???

        override def publishStoredDID(
            did: CanonicalPrismDID
        ): IO[PublishManagedDIDError, ScheduleDIDOperationOutcome] = ???

        override def createAndStoreDID(
            didTemplate: ManagedDIDTemplate
        ): IO[CreateManagedDIDError, LongFormPrismDID] = ???

        override def updateManagedDID(
            did: CanonicalPrismDID,
            actions: Seq[UpdateManagedDIDAction]
        ): IO[UpdateManagedDIDError, ScheduleDIDOperationOutcome] = ???

        override def deactivateManagedDID(
            did: CanonicalPrismDID
        ): IO[UpdateManagedDIDError, ScheduleDIDOperationOutcome] = ???

        override def createAndStorePeerDID(
            serviceEndpoint: java.net.URL
        ): UIO[PeerDID] = ???

        override def getPeerDID(
            didId: DidId
        ): IO[DIDSecretStorageError.KeyNotFoundError, PeerDID] = ???
      }
    }

  def getManagedDIDStateExpectation(createOperation: PrismDIDOperation.Create): Expectation[ManagedDIDService] =
    MockManagedDIDService
      .GetManagedDIDState(
        assertion = Assertion.anything,
        result = Expectation.value(
          Some(
            ManagedDIDState(
              createOperation,
              0,
              PublicationState.Published(scala.collection.immutable.ArraySeq.empty)
            )
          )
        )
      )

  def javaKeyPairWithDIDExpectation(ecKeyPair: Secp256k1KeyPair): Expectation[ManagedDIDService] =
    MockManagedDIDService.JavaKeyPairWithDID(
      assertion = Assertion.anything,
      result = Expectation.value(
        Some((ecKeyPair.privateKey.toJavaPrivateKey, ecKeyPair.publicKey.toJavaPublicKey))
      )
    )
}
