package io.iohk.atala.pollux.core.model.error

import io.iohk.atala.pollux.vc.jwt.W3cCredentialPayload

sealed trait MarkCredentialRecordsAsPublishQueuedError

object MarkCredentialRecordsAsPublishQueuedError {
  final case class RepositoryError(cause: Throwable) extends MarkCredentialRecordsAsPublishQueuedError
  final case class CredentialIdNotDefined(credential: W3cCredentialPayload)
      extends MarkCredentialRecordsAsPublishQueuedError
}
