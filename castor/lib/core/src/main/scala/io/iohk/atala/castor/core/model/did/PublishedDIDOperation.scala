package io.iohk.atala.castor.core.model.did

import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.models.HexStrings.*

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
