package io.iohk.atala.iris.core.service

import cats.Monad
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import io.iohk.atala.iris.core.model.{ConfirmedBlock, ConfirmedIrisBatch}
import io.iohk.atala.iris.core.repository.{DbRepositoryTransactor, IrisBatchesRepository, KeyValueRepository}
import zio.*
import zio.stream.*

trait BlocksSaveSinker {
  val sink: ZSink[Any, Throwable, ConfirmedBlock, Nothing, Unit]
}

object BlocksSaveSinker {
  def layer[F[_]: TagK: Monad, S[_]: TagK]: URLayer[
    KeyValueRepository[F] & IrisBatchesRepository[F, S] & DbRepositoryTransactor[F],
    BlocksSaveSinker
  ] =
    ZLayer.fromFunction((x: KeyValueRepository[F], y: IrisBatchesRepository[F, S], z: DbRepositoryTransactor[F]) =>
      BlocksSaveSinkerImpl(x, y, z)
    )

}

/** @param keyValueRepo
  * @param batchesRepo
  * @param transactor
  * @tparam F
  *   \- a monad which support combining operations which might be performed within one database transaction, like
  *   doobie.ConnectionIO
  * @tparam S
  *   \- type representing a streaming type
  */
class BlocksSaveSinkerImpl[F[_]: Monad, S[_]](
    keyValueRepo: KeyValueRepository[F],
    batchesRepo: IrisBatchesRepository[F, S],
    transactor: DbRepositoryTransactor[F]
) extends BlocksSaveSinker {

  private val LAST_SYNCED_BLOCK_NO = "last_synced_block_no"
  private val LAST_SYNCED_BLOCK_TIMESTAMP = "last_synced_block_timestamp"

  override val sink: ZSink[Any, Throwable, ConfirmedBlock, Nothing, Unit] =
    ZSink.foreach[Any, Throwable, ConfirmedBlock](updateLastSyncedBlock)

  private def updateLastSyncedBlock(block: ConfirmedBlock): Task[Unit] = {
    val timestampEpochMilli = block.blockTimestamp.toEpochMilli
    transactor.runAtomically {
      for {
        _ <- keyValueRepo.set(LAST_SYNCED_BLOCK_NO, Some(block.blockLevel))
        _ <- keyValueRepo.set(LAST_SYNCED_BLOCK_TIMESTAMP, Some(timestampEpochMilli.toString))
        _ <- block.transactions.zipWithIndex.traverse { case ((txId, batch), i) =>
          batchesRepo.saveIrisBatch(ConfirmedIrisBatch(block.blockLevel, block.blockTimestamp, i, txId, batch))
        }
      } yield ()
    }
  }
}
