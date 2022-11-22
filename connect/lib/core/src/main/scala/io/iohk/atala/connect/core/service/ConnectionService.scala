package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionError
import zio._

import java.util.UUID
import io.iohk.atala.mercury.DidComm
import java.{util => ju}
import io.iohk.atala.connect.core.repository.ConnectionRepository

trait ConnectionService {

  def createConnectionInvitation(): IO[ConnectionError, ConnectionRecord]
  def getConnections: IO[ConnectionError, Seq[ConnectionRecord]]
  def getConnectionById(id: UUID): IO[ConnectionError, Option[ConnectionRecord]]
  def deleteConnectionById(id: UUID): IO[ConnectionError, Int]
  def acceptConnectionInvitation(): IO[ConnectionError, ConnectionRecord]
  def markConnectionRequestSent(id: UUID): IO[ConnectionError, ConnectionRecord]
  def receiveConnectionRequest(): IO[ConnectionError, ConnectionRecord]
  def markConnectionResponseSent(id: UUID): IO[ConnectionError, ConnectionRecord]
  def receiveConnectionResponse(): IO[ConnectionError, ConnectionRecord]

}

object ConnectionServiceImpl {
  val layer: URLayer[ConnectionRepository[Task] & DidComm, ConnectionService] =
    ZLayer.fromFunction(ConnectionServiceImpl(_, _))
}

private class ConnectionServiceImpl(
    connectionRepository: ConnectionRepository[Task],
    didComm: DidComm
) extends ConnectionService {

  override def createConnectionInvitation(): IO[ConnectionError, ConnectionRecord] = ???

  override def getConnections: IO[ConnectionError, Seq[ConnectionRecord]] = ???

  override def getConnectionById(id: ju.UUID): IO[ConnectionError, Option[ConnectionRecord]] = ???

  override def deleteConnectionById(id: ju.UUID): IO[ConnectionError, Int] = ???

  override def acceptConnectionInvitation(): IO[ConnectionError, ConnectionRecord] = ???

  override def markConnectionRequestSent(id: ju.UUID): IO[ConnectionError, ConnectionRecord] = ???

  override def receiveConnectionRequest(): IO[ConnectionError, ConnectionRecord] = ???

  override def markConnectionResponseSent(id: ju.UUID): IO[ConnectionError, ConnectionRecord] = ???

  override def receiveConnectionResponse(): IO[ConnectionError, ConnectionRecord] = ???

}
