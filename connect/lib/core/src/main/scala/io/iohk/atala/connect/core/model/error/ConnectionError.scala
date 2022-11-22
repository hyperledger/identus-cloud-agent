package io.iohk.atala.connect.core.model.error

import java.util.UUID

sealed trait ConnectionError

object ConnectionError {
  final case class RepositoryError(cause: Throwable) extends ConnectionError
  final case class RecordIdNotFound(recordId: UUID) extends ConnectionError
  final case class ThreadIdNotFound(thid: UUID) extends ConnectionError
  final case class UnexpectedError(msg: String) extends ConnectionError
}
