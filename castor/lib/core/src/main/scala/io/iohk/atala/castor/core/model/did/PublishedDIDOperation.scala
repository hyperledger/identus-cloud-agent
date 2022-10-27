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
      did: PrismDID,
      updateKey: Base64UrlString,
      previousVersion: HexString,
      delta: UpdateOperationDelta
  ) extends PublishedDIDOperation
}

final case class UpdateOperationDelta(
    patches: Seq[DIDStatePatch],
    updateCommitment: HexString
)

sealed trait DIDStatePatch

object DIDStatePatch {
  final case class AddPublicKeys(publicKeys: Seq[PublicKey]) extends DIDStatePatch
  final case class RemovePublicKeys(ids: Seq[String]) extends DIDStatePatch
  final case class AddServices(services: Seq[Service]) extends DIDStatePatch
  final case class RemoveServices(ids: Seq[String]) extends DIDStatePatch
}

final case class PublishedDIDOperationOutcome(
    did: PrismDID,
    operation: PublishedDIDOperation,
    operationId: HexString
)
