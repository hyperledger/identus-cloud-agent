package io.iohk.atala.iris.sql.repository

import doobie.*
import fs2.Stream
import io.iohk.atala.iris.core.model.ConfirmedIrisBatch
import io.iohk.atala.iris.core.model.ledger.TransactionId
import io.iohk.atala.iris.core.repository.IrisBatchesRepository
import zio.*

class JdbcIrisBatchRepositoryIO extends IrisBatchesRepository[ConnectionIO, StreamIO] {
  override def saveIrisBatch(irisBatch: ConfirmedIrisBatch): ConnectionIO[Unit] = ???

  override def getIrisBatchesStream(lastSeen: Option[TransactionId]): StreamIO[ConfirmedIrisBatch] = ???
}

object JdbcIrisBatchRepositoryIO {
  val layer: ULayer[IrisBatchesRepository[ConnectionIO, StreamIO]] =
    ZLayer.succeed(new JdbcIrisBatchRepositoryIO)
}

class JdbcIrisBatchRepository(xa: Transactor[Task], ioImpl: IrisBatchesRepository[ConnectionIO, StreamIO])
    extends IrisBatchesRepository[Task, StreamZIO] {

  override def saveIrisBatch(irisBatch: ConfirmedIrisBatch): Task[Unit] = ???

  override def getIrisBatchesStream(lastSeen: Option[TransactionId]): StreamZIO[ConfirmedIrisBatch] = ???
}

object JdbcIrisBatchRepository {
  val layer: URLayer[Transactor[Task] & IrisBatchesRepository[ConnectionIO, StreamIO], IrisBatchesRepository[
    Task,
    StreamZIO
  ]] =
    ZLayer.fromFunction(new JdbcIrisBatchRepository(_, _))
}
