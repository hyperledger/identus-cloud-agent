package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.prism.crypto.Sha256

import scala.collection.compat.immutable.ArraySeq
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.shared.models.HexStrings.HexString

import io.lemonlabs.uri.Uri

sealed trait PrismDIDOperation {
  def did: CanonicalPrismDID
  def toAtalaOperation: node_models.AtalaOperation
  def toAtalaOperationHash: Array[Byte] = Sha256.compute(toAtalaOperation.toByteArray).getValue
}

object PrismDIDOperation extends ProtoModelHelper {
  final case class Create(publicKeys: Seq[PublicKey | InternalPublicKey], services: Seq[Service], context: Seq[String])
      extends PrismDIDOperation {
    override def toAtalaOperation: node_models.AtalaOperation = node_models.AtalaOperation(this.toProto)
    override def did: CanonicalPrismDID = PrismDID.buildLongFormFromOperation(this).asCanonical
  }

  final case class Update(did: CanonicalPrismDID, previousOperationHash: ArraySeq[Byte], actions: Seq[UpdateDIDAction])
      extends PrismDIDOperation {
    override def toAtalaOperation: node_models.AtalaOperation = node_models.AtalaOperation(this.toProto)
  }

  final case class Deactivate(did: CanonicalPrismDID, previousOperationHash: ArraySeq[Byte]) extends PrismDIDOperation {
    override def toAtalaOperation: node_models.AtalaOperation = node_models.AtalaOperation(this.toProto)
  }
}

final case class SignedPrismDIDOperation(
    operation: PrismDIDOperation,
    signature: ArraySeq[Byte],
    signedWithKey: String
) {
  def toSignedAtalaOperation: node_models.SignedAtalaOperation = {
    import ProtoModelHelper.*
    this.toProto
  }
  def toAtalaOperationId: Array[Byte] = Sha256.compute(toSignedAtalaOperation.toByteArray).getValue
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

sealed trait UpdateDIDAction
object UpdateDIDAction {
  final case class AddKey(publicKey: PublicKey) extends UpdateDIDAction
  final case class AddInternalKey(publicKey: InternalPublicKey) extends UpdateDIDAction

  /** Can be used for both PublicKey and InternalPublicKey */
  final case class RemoveKey(id: String) extends UpdateDIDAction
  final case class AddService(service: Service) extends UpdateDIDAction
  final case class RemoveService(id: String) extends UpdateDIDAction
  final case class UpdateService(
      id: String,
      `type`: Option[ServiceType] = None,
      endpoint: Option[ServiceEndpoint] = None
  ) extends UpdateDIDAction
  final case class PatchContext(context: Seq[String]) extends UpdateDIDAction
}
