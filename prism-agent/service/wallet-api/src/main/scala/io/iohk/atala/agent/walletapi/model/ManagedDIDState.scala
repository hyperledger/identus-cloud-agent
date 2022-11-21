package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.castor.core.model.did.PrismDIDOperation

import scala.collection.immutable.ArraySeq

sealed trait ManagedDIDState

object ManagedDIDState {
  final case class Created(operation: PrismDIDOperation.Create) extends ManagedDIDState
  final case class PublicationPending(operation: PrismDIDOperation.Create, operationId: ArraySeq[Byte])
      extends ManagedDIDState
  final case class Published(operation: PrismDIDOperation.Create, operationId: ArraySeq[Byte]) extends ManagedDIDState
}
