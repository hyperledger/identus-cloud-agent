package io.iohk.atala.pollux.core.model.error

sealed trait PublishCredentialBatchError

object PublishCredentialBatchError {
  final case class IrisError(cause: Throwable) extends PublishCredentialBatchError
}
