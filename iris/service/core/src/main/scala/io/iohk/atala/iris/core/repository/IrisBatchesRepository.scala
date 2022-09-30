package io.iohk.atala.iris.core.repository

import io.iohk.atala.iris.core.model.ledger.TransactionId
import io.iohk.atala.iris.proto.dlt as proto

import java.time.Instant

trait IrisBatchesRepository[F[_]] {
  def saveIrisBatch(
      blockLevel: Int,
      blockTransaction: Instant,
      transactionSeqId: Int,
      batchId: TransactionId,
      batch: proto.IrisBatch
  ): F[Unit]
}
