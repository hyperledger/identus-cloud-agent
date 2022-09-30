package io.iohk.atala.iris.core.service

import cats.Monad
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import io.iohk.atala.iris.core.model.ConfirmedBlock
import io.iohk.atala.iris.core.repository.{DbTransactor, IrisBatchesRepository, KeyValueRepository}
import zio.*
import zio.stream.*

trait BlocksSaveSinker {
  val sink: ZSink[Any, Throwable, ConfirmedBlock, Nothing, Unit]
}

object BlocksSaveSinker {
  def layer[F[_]: TagK: Monad]: URLayer[
    KeyValueRepository[F] & IrisBatchesRepository[F] & DbTransactor[F],
    BlocksSaveSinker
  ] =
    ZLayer.fromFunction((x: KeyValueRepository[F], y: IrisBatchesRepository[F], z: DbTransactor[F]) =>
      BlocksSaveSinkerImpl(x, y, z)
    )

}

class BlocksSaveSinkerImpl[F[_]: Monad](
    keyValueRepo: KeyValueRepository[F],
    batchesRepo: IrisBatchesRepository[F],
    transactor: DbTransactor[F]
) extends BlocksSaveSinker {

  private val LAST_SYNCED_BLOCK_NO = "last_synced_block_no"
  private val LAST_SYNCED_BLOCK_TIMESTAMP = "last_synced_block_timestamp"

  override val sink: ZSink[Any, Throwable, ConfirmedBlock, Nothing, Unit] =
    ZSink.foreach[Any, Throwable, ConfirmedBlock](updateLastSyncedBlock)

  private def updateLastSyncedBlock(block: ConfirmedBlock): Task[Unit] = {
    val timestampEpochMilli = block.blockTimestamp
    transactor.runAtomically {
      for {
        _ <- keyValueRepo.set(LAST_SYNCED_BLOCK_NO, Some(block.blockLevel))
        _ <- keyValueRepo.set(LAST_SYNCED_BLOCK_TIMESTAMP, Some(timestampEpochMilli.toString))
        _ <- block.transactions.zipWithIndex.traverse { case ((txId, batch), i) =>
          batchesRepo.saveIrisBatch(block.blockLevel, block.blockTimestamp, i, txId, batch)
        }
      } yield ()
    }
  }
}
