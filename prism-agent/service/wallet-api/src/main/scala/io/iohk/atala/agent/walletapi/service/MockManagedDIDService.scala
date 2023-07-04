package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.error.*
import io.iohk.atala.agent.walletapi.model.{
  ManagedDIDDetail,
  ManagedDIDState,
  ManagedDIDTemplate,
  UpdateManagedDIDAction
}
import io.iohk.atala.agent.walletapi.storage.DIDNonSecretStorage
import io.iohk.atala.castor.core.model.did.{CanonicalPrismDID, LongFormPrismDID, ScheduleDIDOperationOutcome}
import io.iohk.atala.mercury.PeerDID
import io.iohk.atala.mercury.model.DidId
import zio.mock.*
import zio.{mock, *}

import java.security.{PrivateKey as JavaPrivateKey, PublicKey as JavaPublicKey}

object MockManagedDIDService extends Mock[ManagedDIDService] {

  object GetManagedDIDState extends Effect[CanonicalPrismDID, GetManagedDIDError, Option[ManagedDIDState]]

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
        ): IO[GetKeyError, Option[(JavaPrivateKey, JavaPublicKey)]] = ???

        override def getManagedDIDState(
            did: CanonicalPrismDID
        ): IO[GetManagedDIDError, Option[ManagedDIDState]] = proxy(GetManagedDIDState, did)

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
            serviceEndpoint: String
        ): UIO[PeerDID] = ???

        override def getPeerDID(
            didId: DidId
        ): IO[DIDSecretStorageError.KeyNotFoundError, PeerDID] = ???
      }
    }
}
