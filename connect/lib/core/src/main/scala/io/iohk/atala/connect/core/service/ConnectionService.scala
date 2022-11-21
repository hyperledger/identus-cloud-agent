package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionError
import zio.IO

import java.util.UUID

trait ConnectionsService {

  def createConnectionInvitation: IO[ConnectionError, ConnectionRecord]
  def getConnections: IO[ConnectionError, Seq[ConnectionRecord]]
  def getConnectionById(id: UUID): IO[ConnectionError, Option[ConnectionRecord]]
  def deleteConnectionById(id: UUID): IO[ConnectionError, Int]
  def acceptConnectionInvitation(): IO[ConnectionError, ConnectionRecord]
  def markConnectionRequestSent(id: UUID): IO[ConnectionError, ConnectionRecord]
  def receiveConnectionRequest(): IO[ConnectionError, ConnectionRecord]
  def markConnectionResponseSent(id: UUID): IO[ConnectionError, ConnectionRecord]
  def receiveConnectionResponse(): IO[ConnectionError, ConnectionRecord]

}
