package io.iohk.atala.castor.core.model.did

import scala.collection.compat.immutable.ArraySeq

sealed trait PrismDIDOperation

object PrismDIDOperation {
  final case class Create(publicKeys: Seq[PublicKey], internalKeys: Seq[InternalPublicKey]) extends PrismDIDOperation
}

sealed trait SignedPrismDIDOperation

object SignedPrismDIDOperation {
  final case class Create(operation: PrismDIDOperation.Create, signature: ArraySeq[Byte], signedWithKey: String)
      extends SignedPrismDIDOperation
}

final case class PublishedDIDOperationOutcome(
    did: PrismDID,
    operation: PrismDIDOperation,
    operationId: ArraySeq[Byte]
)
