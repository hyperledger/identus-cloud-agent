package io.iohk.atala.connect.core.model.error

import java.util.UUID
import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState

sealed trait ConnectionServiceError

object ConnectionServiceError {
  final case class RepositoryError(cause: Throwable) extends ConnectionServiceError
  final case class RecordIdNotFound(recordId: UUID) extends ConnectionServiceError
  final case class RecordWithStateNotFound(recordId: UUID, state: ProtocolState) extends ConnectionServiceError
  final case class ThreadIdNotFound(thid: String) extends ConnectionServiceError
  final case class InvitationParsingError(cause: Throwable) extends ConnectionServiceError
  final case class UnexpectedError(msg: String) extends ConnectionServiceError
  final case class InvalidStateForOperation(msg: String) extends ConnectionServiceError
  final case class InvitationAlreadyReceived(msg: String) extends ConnectionServiceError
  final case class InvitationExpired(msg: String) extends ConnectionServiceError

}
