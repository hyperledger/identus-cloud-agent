package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.castor.core.model.did.{CanonicalPrismDID, PrismDIDOperation, ScheduledDIDOperationStatus}
import zio.*

import java.time.Instant
import scala.collection.immutable.ArraySeq

final case class ManagedDIDDetail(did: CanonicalPrismDID, state: ManagedDIDState)

sealed trait ManagedDIDState {
  def createOperation: PrismDIDOperation.Create
}

object ManagedDIDState {
  final case class Created(createOperation: PrismDIDOperation.Create) extends ManagedDIDState
  final case class PublicationPending(createOperation: PrismDIDOperation.Create, publishOperationId: ArraySeq[Byte])
      extends ManagedDIDState
  final case class Published(createOperation: PrismDIDOperation.Create, publishOperationId: ArraySeq[Byte])
      extends ManagedDIDState
}

final case class DIDUpdateLineage(
    operationId: ArraySeq[Byte],
    operationHash: ArraySeq[Byte],
    previousOperationHash: ArraySeq[Byte],
    status: ScheduledDIDOperationStatus,
    createdAt: Instant,
    updatedAt: Instant
)
