package io.iohk.atala.pollux.core.model.error

import io.iohk.atala.pollux.core.model.DidCommID

sealed trait PresentationError

object PresentationError {
  final case class RepositoryError(cause: Throwable) extends PresentationError
  final case class RecordIdNotFound(recordId: DidCommID) extends PresentationError
  final case class ThreadIdNotFound(thid: DidCommID) extends PresentationError
  final case class InvalidFlowStateError(msg: String) extends PresentationError
  final case class UnexpectedError(msg: String) extends PresentationError
  final case class IssuedCredentialNotFoundError(cause: Throwable) extends PresentationError
  final case class PresentationDecodingError(cause: Throwable) extends PresentationError
  final case class PresentationNotFoundError(cause: Throwable) extends PresentationError
  final case class HolderBindingError(msg: String) extends PresentationError
}
