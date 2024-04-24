package org.hyperledger.identus.agent.walletapi.model.error

import org.hyperledger.identus.mercury.model.DidId

sealed trait DIDSecretStorageError extends Throwable

object DIDSecretStorageError {
  case class KeyNotFoundError(didId: DidId, keyId: String) extends DIDSecretStorageError
  case class WalletNotFoundError(didId: DidId) extends DIDSecretStorageError
}
