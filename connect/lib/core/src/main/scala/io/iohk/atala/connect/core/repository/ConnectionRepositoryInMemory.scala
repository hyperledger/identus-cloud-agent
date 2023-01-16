package io.iohk.atala.connect.core.repository

import zio._
import io.iohk.atala.mercury.protocol.connection.ConnectionRequest
import io.iohk.atala.mercury.protocol.connection.ConnectionResponse
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState
import java.util.UUID
import java.time.Instant

class ConnectionRepositoryInMemory(storeRef: Ref[Map[UUID, ConnectionRecord]]) extends ConnectionRepository[Task] {

  override def updateWithConnectionResponse(
      recordId: UUID,
      response: ConnectionResponse,
      state: ProtocolState
  ): Task[Int] = {
    for {
      maybeRecord <- getConnectionRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
                record.copy(
                  updatedAt = Some(Instant.now),
                  connectionResponse = Some(response),
                  protocolState = state
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(1))
    } yield count
  }

  override def updateConnectionProtocolState(recordId: UUID, from: ProtocolState, to: ProtocolState): Task[Int] = {
    for {
      store <- storeRef.get
      maybeRecord <- ZIO.succeed(
        store.find((uuid, record) => uuid == recordId && record.protocolState == from).map(_._2)
      )
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r => r.updated(recordId, record.copy(protocolState = to)))
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def deleteConnectionRecord(recordId: UUID): Task[Int] = {
    for {
      maybeRecord <- getConnectionRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r => r.removed(recordId))
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def updateWithConnectionRequest(
      recordId: UUID,
      request: ConnectionRequest,
      state: ProtocolState
  ): Task[Int] = {
    for {
      maybeRecord <- getConnectionRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
                record.copy(
                  updatedAt = Some(Instant.now),
                  connectionRequest = Some(request),
                  protocolState = state
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(1))
    } yield count
  }

  override def getConnectionRecordByThreadId(thid: UUID): Task[Option[ConnectionRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values.find(_.thid == Some(thid))
  }

  override def getConnectionRecords(): Task[Seq[ConnectionRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values.toSeq
  }

  override def createConnectionRecord(record: ConnectionRecord): Task[Int] = {
    for {
      _ <- storeRef.update(r => r + (record.id -> record))
    } yield 1
  }

  override def getConnectionRecord(recordId: UUID): Task[Option[ConnectionRecord]] = {
    for {
      store <- storeRef.get
      record = store.get(recordId)
    } yield record
  }

}

object ConnectionRepositoryInMemory {
  val layer: ULayer[ConnectionRepository[Task]] = ZLayer.fromZIO(
    Ref
      .make(Map.empty[UUID, ConnectionRecord])
      .map(ConnectionRepositoryInMemory(_))
  )
}
