package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.ProtoModelHelper

import scala.collection.compat.immutable.ArraySeq
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.AtalaOperation

sealed trait PrismDIDOperation {
  def toAtalaOperation: node_models.AtalaOperation
}

object PrismDIDOperation extends ProtoModelHelper {
  final case class Create(publicKeys: Seq[PublicKey], internalKeys: Seq[InternalPublicKey]) extends PrismDIDOperation {
    override def toAtalaOperation: AtalaOperation = node_models.AtalaOperation(this.toProto)
  }
}

sealed trait SignedPrismDIDOperation

object SignedPrismDIDOperation {
  final case class Create(operation: PrismDIDOperation.Create, signature: ArraySeq[Byte], signedWithKey: String)
      extends SignedPrismDIDOperation
}

final case class ScheduleDIDOperationOutcome(
    did: PrismDID,
    operation: PrismDIDOperation,
    operationId: ArraySeq[Byte]
)
