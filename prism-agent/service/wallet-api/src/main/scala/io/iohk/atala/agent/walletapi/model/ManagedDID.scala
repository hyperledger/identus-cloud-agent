package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.castor.core.model.did.{CanonicalPrismDID, PrismDIDOperation}

import scala.collection.immutable.ArraySeq

final case class ManagedDIDDetail(did: CanonicalPrismDID, state: ManagedDIDState)

sealed trait ManagedDIDState

object ManagedDIDState {
  final case class Created(operation: PrismDIDOperation.Create) extends ManagedDIDState
  final case class PublicationPending(operation: PrismDIDOperation.Create, operationId: ArraySeq[Byte])
      extends ManagedDIDState
  final case class Published(operation: PrismDIDOperation.Create, operationId: ArraySeq[Byte]) extends ManagedDIDState
}
