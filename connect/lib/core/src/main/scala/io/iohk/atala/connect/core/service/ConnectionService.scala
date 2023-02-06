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
  ): IO[ConnectionServiceError, Option[ConnectionRecord]]

  def markConnectionRequestSent(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]]

  def receiveConnectionRequest(request: ConnectionRequest): IO[ConnectionServiceError, Option[ConnectionRecord]]

  def acceptConnectionRequest(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]]

  def markConnectionResponseSent(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]]

  def receiveConnectionResponse(response: ConnectionResponse): IO[ConnectionServiceError, Option[ConnectionRecord]]

  def getConnectionRecords(): IO[ConnectionServiceError, Seq[ConnectionRecord]]

  def getConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]]

  def deleteConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Int]

  def updateAfterFail(recordId: UUID, failReason: Option[String]): IO[ConnectionServiceError, Int]

}
