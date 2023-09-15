package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.model.DIDPublicKeyTemplate
import io.iohk.atala.agent.walletapi.model.DIDUpdateLineage
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.agent.walletapi.model.ManagedDIDTemplate
import io.iohk.atala.agent.walletapi.model.PublicationState
import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.util.OperationFactory
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.did.PrismDIDOperation
import io.iohk.atala.castor.core.model.did.ScheduledDIDOperationStatus
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import zio.test.*

import java.time.Instant
import scala.collection.immutable.ArraySeq

trait StorageSpecHelper extends ApolloSpecHelper {
  protected val didExample = PrismDID.buildLongFormFromOperation(PrismDIDOperation.Create(Nil, Nil, Nil))

  protected def updateLineage(
      operationId: Array[Byte] = Array.fill(32)(0),
      operationHash: Array[Byte] = Array.fill(32)(0),
      status: ScheduledDIDOperationStatus = ScheduledDIDOperationStatus.Confirmed
  ) = DIDUpdateLineage(
    operationId = ArraySeq.from(operationId),
    operationHash = ArraySeq.from(operationHash),
    previousOperationHash = ArraySeq.fill(32)(0),
    status = status,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH
  )

  protected def generateKeyPair() = apollo.ecKeyFactory.generateKeyPair(EllipticCurve.SECP256K1)

  protected def generateCreateOperation(keyIds: Seq[String]) =
    OperationFactory(apollo).makeCreateOperationRandKey("master0")(
      ManagedDIDTemplate(
        publicKeys = keyIds.map(DIDPublicKeyTemplate(_, VerificationRelationship.Authentication)),
        services = Nil,
        contexts = Nil
      )
    )

  protected def generateCreateOperationHdKey(keyIds: Seq[String], didIndex: Int) =
    OperationFactory(apollo).makeCreateOperationHdKey("master0", Array.fill(64)(0))(
      didIndex,
      ManagedDIDTemplate(
        publicKeys = keyIds.map(DIDPublicKeyTemplate(_, VerificationRelationship.Authentication)),
        services = Nil,
        contexts = Nil
      )
    )

  protected def initializeDIDStateAndKeys(keyIds: Seq[String] = Nil, didIndex: Int) = {
    for {
      nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
      generated <- generateCreateOperationHdKey(keyIds, didIndex)
      (createOperation, hdKeys) = generated
      did = createOperation.did
      _ <- nonSecretStorage.insertManagedDID(
        did,
        ManagedDIDState(createOperation, didIndex, PublicationState.Created()),
        hdKeys.keyPaths ++ hdKeys.internalKeyPaths
      )
    } yield did
  }

  extension [R, E](spec: Spec[R & WalletAccessContext, E]) {
    def globalWallet: Spec[R & WalletManagementService, E] = {
      spec.provideSomeLayer(
        ZLayer.fromZIO(
          ZIO
            .serviceWithZIO[WalletManagementService](_.createWallet(Wallet("global-wallet")))
            .map(wallet => WalletAccessContext(wallet.id))
            .mapError(_.toThrowable)
            .orDie
        )
      )
    }
  }
}
