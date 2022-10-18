package io.iohk.atala.iris.core.repository

import io.iohk.atala.iris.core.model.ConfirmedIrisBatch
import io.iohk.atala.iris.core.model.ledger.TransactionId
import io.iohk.atala.iris.proto.dlt as proto

import java.time.Instant

trait ROIrisBatchesRepository[S[_]] {

  // Streams batches which are already on the database
  // Every transaction contains a IrisBatch in its metadata, hence,
  // there is one to one correspondence between TransactionId and IrisBatch
  def getIrisBatchesStream(lastSeen: Option[TransactionId]): S[ConfirmedIrisBatch]
}

/**
 * @tparam F represents a monad where CRUD requests are executed
 * @tparam S represents a monad for streaming of data
 */
trait IrisBatchesRepository[F[_], S[_]] extends ROIrisBatchesRepository[S] {
  def saveIrisBatch(irisBatch: ConfirmedIrisBatch): F[Unit]
}
