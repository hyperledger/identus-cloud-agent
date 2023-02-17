package io.iohk.atala.pollux.core.model.error

import java.util.UUID

sealed trait PresentationError

object PresentationError {
  final case class RepositoryError(cause: Throwable) extends PresentationError
  final case class RecordIdNotFound(recordId: UUID) extends PresentationError
  final case class ThreadIdNotFound(thid: UUID) extends PresentationError
  final case class InvalidFlowStateError(msg: String) extends PresentationError
  final case class UnexpectedError(msg: String) extends PresentationError
  final case class IssuedCredentialNotFoundError(cause: Throwable) extends PresentationError
  final case class PresentationDecodingError(cause: Throwable) extends PresentationError
  final case class PresentationNotFoundError(cause: Throwable) extends PresentationError
  final case class HolderBindingError(msg: String) extends PresentationError
}
