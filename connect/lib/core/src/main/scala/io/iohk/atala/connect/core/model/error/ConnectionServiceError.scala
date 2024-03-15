package io.iohk.atala.connect.core.model.error

import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState

import java.util.UUID

sealed trait ConnectionServiceError

object ConnectionServiceError {
  final case class InvitationParsingError(cause: String) extends ConnectionServiceError
  final case class InvitationAlreadyReceived(invitationId: String) extends ConnectionServiceError
  final case class RecordIdNotFound(recordId: UUID) extends ConnectionServiceError
  final case class ThreadIdNotFound(thid: String) extends ConnectionServiceError
  final case class ThreadIdMissingInReceivedMessage(msgId: String) extends ConnectionServiceError
  final case class InvalidStateForOperation(state: ProtocolState) extends ConnectionServiceError
  final case class InvitationExpired(invitationId: String) extends ConnectionServiceError
}
