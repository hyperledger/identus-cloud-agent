package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionError
import zio._
import java.util.UUID
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import io.iohk.atala.mercury.protocol.connection.ConnectionRequest
import io.iohk.atala.mercury.protocol.connection.ConnectionResponse

trait ConnectionService {

  def createConnectionInvitation(label: Option[String]): IO[ConnectionError, ConnectionRecord]

  def receiveConnectionInvitation(invitation: String): IO[ConnectionError, ConnectionRecord]

  def acceptConnectionInvitation(recordId: UUID): IO[ConnectionError, Option[ConnectionRecord]]

  def markConnectionRequestSent(recordId: UUID): IO[ConnectionError, Option[ConnectionRecord]]

  def receiveConnectionRequest(request: ConnectionRequest): IO[ConnectionError, Option[ConnectionRecord]]

  def markConnectionResponseSent(recordId: UUID): IO[ConnectionError, Option[ConnectionRecord]]

  def receiveConnectionResponse(response: ConnectionResponse): IO[ConnectionError, Option[ConnectionRecord]]

  def getConnectionRecords(): IO[ConnectionError, Seq[ConnectionRecord]]

  def getConnectionRecord(recordId: UUID): IO[ConnectionError, Option[ConnectionRecord]]

  def deleteConnectionRecord(recordId: UUID): IO[ConnectionError, Int]

}
