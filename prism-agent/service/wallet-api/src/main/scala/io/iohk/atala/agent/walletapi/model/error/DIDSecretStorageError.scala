package io.iohk.atala.agent.walletapi.model.error

import io.iohk.atala.mercury.model.DidId

sealed trait DIDSecretStorageError extends Throwable

object DIDSecretStorageError {
  case class KeyNotFoundError(didId: DidId, keyId: String) extends DIDSecretStorageError
}
