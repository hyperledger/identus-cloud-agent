package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.did.PrismDIDOperation
import io.iohk.atala.castor.core.model.did.ScheduledDIDOperationStatus
import io.iohk.atala.agent.walletapi.model.DIDUpdateLineage
import scala.collection.immutable.ArraySeq
import java.time.Instant
import io.iohk.atala.agent.walletapi.crypto.KeyGeneratorWrapper
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.agent.walletapi.util.OperationFactory
import io.iohk.atala.agent.walletapi.model.ManagedDIDTemplate
import io.iohk.atala.agent.walletapi.model.DIDPublicKeyTemplate
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import zio.*
import io.iohk.atala.agent.walletapi.model.ManagedDIDState

trait StorageSpecHelper {
  protected val didExample = PrismDID.buildLongFormFromOperation(PrismDIDOperation.Create(Nil, Nil))

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

  protected def generateKeyPair() = KeyGeneratorWrapper.generateECKeyPair(EllipticCurve.SECP256K1)

  protected def generateCreateOperation(keyIds: Seq[String]) =
    OperationFactory.makeCreateOperation("master0", EllipticCurve.SECP256K1, generateKeyPair)(
      ManagedDIDTemplate(
        publicKeys = keyIds.map(DIDPublicKeyTemplate(_, VerificationRelationship.Authentication)),
        services = Nil
      )
    )

  protected def initializeDIDStateAndKeys(keyIds: Seq[String] = Nil) = {
    for {
      nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
      secretStorage <- ZIO.service[DIDSecretStorage]
      generated <- generateCreateOperation(keyIds)
      (createOperation, secrets) = generated
      did = createOperation.did
      keyPairs = secrets.keyPairs.toSeq
      _ <- nonSecretStorage.setManagedDIDState(did, ManagedDIDState.Created(createOperation))
      _ <- ZIO.foreach(keyPairs) { case (keyId, keyPair) =>
        secretStorage.insertKey(did, keyId, keyPair, createOperation.toAtalaOperationHash)
      }
    } yield (did, keyPairs)
  }
}
