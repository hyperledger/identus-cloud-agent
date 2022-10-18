package io.iohk.atala.iris.core.mock

import io.iohk.atala.iris.core.model.ConfirmedIrisBatch
import io.iohk.atala.iris.core.model.ledger.TransactionId
import io.iohk.atala.iris.core.repository.IrisBatchesRepository
import zio.*
import zio.stream.*

type StreamZIO[A] = Stream[Throwable, A]

object InMemoryIrisBatchesRepository {
  val layer: ULayer[InMemoryIrisBatchesRepository] = ZLayer.fromZIO {
    for {
      ref <- Ref.make(Vector[ConfirmedIrisBatch]())
      srv = InMemoryIrisBatchesRepository(ref)
    } yield srv
  }
}

class InMemoryIrisBatchesRepository(list: Ref[Vector[ConfirmedIrisBatch]])
    extends IrisBatchesRepository[Task, StreamZIO] {
  override def saveIrisBatch(irisBatch: ConfirmedIrisBatch): Task[Unit] = list.update(_.appended(irisBatch))

  override def getIrisBatchesStream(lastSeen: Option[TransactionId]): StreamZIO[ConfirmedIrisBatch] =
    ZStream.fromIterableZIO(list.get)
    
  def getConfirmedBatches: Task[Vector[ConfirmedIrisBatch]] = list.get
}
