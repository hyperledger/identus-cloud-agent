package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.ProtoModelHelper

import scala.collection.compat.immutable.ArraySeq
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.AtalaOperation

sealed trait PrismDIDOperation {
  def toAtalaOperation: node_models.AtalaOperation
}

object PrismDIDOperation extends ProtoModelHelper {
  final case class Create(publicKeys: Seq[PublicKey], internalKeys: Seq[InternalPublicKey], services: Seq[Service])
      extends PrismDIDOperation {
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

enum ScheduledDIDOperationStatus {
  case Pending extends ScheduledDIDOperationStatus
  case AwaitingConfirmation extends ScheduledDIDOperationStatus
  case Confirmed extends ScheduledDIDOperationStatus
  case Rejected extends ScheduledDIDOperationStatus
}

final case class ScheduledDIDOperationDetail(
    status: ScheduledDIDOperationStatus
)
