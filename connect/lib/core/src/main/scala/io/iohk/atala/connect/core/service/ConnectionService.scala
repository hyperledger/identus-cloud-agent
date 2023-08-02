package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.connection.{ConnectionRequest, ConnectionResponse}
import zio.*

import java.util.UUID

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
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: ConnectionRecord.ProtocolState*
  ): IO[ConnectionServiceError, Seq[ConnectionRecord]]

  def getConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]]

  def getConnectionRecordByThreadId(thid: String): IO[ConnectionServiceError, Option[ConnectionRecord]]

  def deleteConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Int]

  def reportProcessingFailure(recordId: UUID, failReason: Option[String]): IO[ConnectionServiceError, Unit]

}
