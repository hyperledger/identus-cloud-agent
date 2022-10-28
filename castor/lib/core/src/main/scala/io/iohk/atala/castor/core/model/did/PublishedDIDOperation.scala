package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.models.HexStrings.*

import scala.collection.immutable.ArraySeq

object DIDOperationHashes {

  opaque type DIDOperationHash = ArraySeq[Byte]

  object DIDOperationHash {
    def fromByteArray(bytes: Array[Byte]): DIDOperationHash = ArraySeq.from(bytes)

    def fromOperation(op: PublishedDIDOperation): DIDOperationHash = {
      import ProtoModelHelper.*
      val bytes = op match {
        case op: PublishedDIDOperation.Create => op.toProto.toByteArray
        case op: PublishedDIDOperation.Update => op.toProto.toByteArray
      }
      fromByteArray(Sha256.compute(bytes).getValue)
    }
  }

  extension (h: DIDOperationHash) {
    def toByteArray: Array[Byte] = h.toArray
    def toHexString: HexString = HexString.fromByteArray(toByteArray)
  }

}

sealed trait PublishedDIDOperation

object PublishedDIDOperation {
  final case class Create(
      updateCommitment: HexString,
      recoveryCommitment: HexString,
      storage: DIDStorage.Cardano,
      document: DIDDocument
  ) extends PublishedDIDOperation

  final case class Update(
      did: PrismDIDV1,
      updateKey: Base64UrlString,
      previousVersion: HexString,
      delta: UpdateOperationDelta,
      signature: Base64UrlString
  ) extends PublishedDIDOperation
}

final case class UpdateOperationDelta(
    patches: Seq[DIDStatePatch],
    updateCommitment: HexString
)

sealed trait DIDStatePatch

object DIDStatePatch {
  final case class AddPublicKey(publicKey: PublicKey) extends DIDStatePatch
  final case class RemovePublicKey(id: String) extends DIDStatePatch
  final case class AddService(service: Service) extends DIDStatePatch
  final case class RemoveService(id: String) extends DIDStatePatch
}

final case class PublishedDIDOperationOutcome(
    did: PrismDID,
    operation: PublishedDIDOperation,
    operationId: HexString
)
