package io.iohk.atala.agent.walletapi.model.error

import io.iohk.atala.mercury.model.DidId

sealed trait ManagedDIDServiceError extends Throwable

object ManagedDIDServiceError {
  case class PeerDIDNotFoundError(didId: DidId) extends ManagedDIDServiceError
}
