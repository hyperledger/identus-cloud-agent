package io.iohk.atala.agent.walletapi.model.error

import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.error.DIDOperationError

import scala.collection.immutable.ArraySeq

sealed trait PublishManagedDIDError

object PublishManagedDIDError {
  final case class DIDNotFound(did: PrismDID) extends PublishManagedDIDError
  final case class WalletStorageError(cause: Throwable) extends PublishManagedDIDError
  final case class OperationError(cause: DIDOperationError) extends PublishManagedDIDError
  final case class CryptographicError(cause: Throwable) extends PublishManagedDIDError
  final case class AwaitingPublication(operationId: ArraySeq[Byte]) extends PublishManagedDIDError
  final case class AlreadyPublished(operationId: ArraySeq[Byte]) extends PublishManagedDIDError
}
