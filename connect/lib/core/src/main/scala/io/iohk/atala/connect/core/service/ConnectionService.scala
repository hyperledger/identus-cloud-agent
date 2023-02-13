package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import zio._
import java.util.UUID
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import io.iohk.atala.mercury.protocol.connection.ConnectionRequest
import io.iohk.atala.mercury.protocol.connection.ConnectionResponse

trait ConnectionService {

  def createConnectionInvitation(
      label: Option[String],
      pairwiseDID: DidId
  ): IO[ConnectionServiceError, ConnectionRecord]

  def receiveConnectionInvitation(invitation: String): IO[ConnectionServiceError, ConnectionRecord]

  def acceptConnectionInvitation(
      recordId: UUID,
      pairwiseDid: DidId
  ): IO[ConnectionServiceError, ConnectionRecord]

  def markConnectionRequestSent(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord]

  def receiveConnectionRequest(request: ConnectionRequest): IO[ConnectionServiceError, ConnectionRecord]

  def acceptConnectionRequest(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord]

  def markConnectionResponseSent(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord]

  def receiveConnectionResponse(response: ConnectionResponse): IO[ConnectionServiceError, ConnectionRecord]

  def getConnectionRecords(): IO[ConnectionServiceError, Seq[ConnectionRecord]]

  def getConnectionRecordsByStates(
      states: ConnectionRecord.ProtocolState*
  ): IO[ConnectionServiceError, Seq[ConnectionRecord]]

  /** Get the ConnectionRecord by the record id. If the record is id is not found the value None will be return */
  def getConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]]

  def deleteConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Int]

  def reportProcessingFailure(recordId: UUID, failReason: Option[String]): IO[ConnectionServiceError, Int]

}
