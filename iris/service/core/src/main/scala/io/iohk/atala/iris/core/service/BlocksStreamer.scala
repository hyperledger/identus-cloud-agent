package io.iohk.atala.iris.core.service

import io.iohk.atala.iris.core.model.ConfirmedBlock
import io.iohk.atala.iris.core.model.ledger.{Block, Ledger, TransactionId, TransactionMetadata}
import io.iohk.atala.iris.core.repository.{ROBlocksRepository, ROKeyValueRepository}
import io.iohk.atala.iris.proto.dlt as proto
import zio.*
import zio.stream.*

trait BlocksStreamer {
  val stream: UStream[ConfirmedBlock]
}

object BlocksStreamer {
  case class Config(targetLedger: Ledger, genesisBlockNumber: Int, blockConfirmationsToWait: Int, blockEvery: Duration)

  def layer(
      config: Config
  ): URLayer[ROBlocksRepository[UIO] & ROKeyValueRepository[UIO], BlocksStreamer] =
    ZLayer.fromFunction(BlocksStreamerImpl(_, _, config))
}

/** The goal of this streaming service is to emit batches of operations which are confirmed in the blockchain. It
  * stateful and reply on block read only database and key value read only database.
  * @param blocksRep
  *   \- read only storage of blocks from the blockchain
  * @param keyValueRep
  *   \- read only key-value storage
  * @param config
  *   \- protocol specific constants
  */
class BlocksStreamerImpl(
    val blocksRep: ROBlocksRepository[UIO],
    val keyValueRep: ROKeyValueRepository[UIO],
    val config: BlocksStreamer.Config
) extends BlocksStreamer {
  private val LAST_SYNCED_BLOCK_NO = "last_synced_block_no"
  private val LAST_SYNCED_BLOCK_TIMESTAMP = "last_synced_block_timestamp"
  private val MAX_SYNC_BLOCKS = 100

  private sealed trait BlocksSyncOutcome

  private object BlocksSyncOutcome {
    case object MoreBlocksToSyncExist extends BlocksSyncOutcome
    case object NoMoreBlocks extends BlocksSyncOutcome
  }

  private type ConfirmedBlockCallback = ZStream.Emit[Any, Nothing, ConfirmedBlock, Unit]

  override val stream: UStream[ConfirmedBlock] = ZStream.asyncZIO[Any, Nothing, ConfirmedBlock] { cb =>
    startSyncing().provideLayer(ZLayer.succeed(cb)).fork
  }

  private def startSyncing(): URIO[ConfirmedBlockCallback, Unit] = {
    for {
      outcome <- syncMissingBlocks()
      _ <-
        if (outcome == BlocksSyncOutcome.NoMoreBlocks) {
          ZIO.sleep(config.blockEvery).flatMap(_ => startSyncing())
        } else startSyncing()
    } yield ()
  }

  /** Sync up on blocks from the blockchain and returns whether there are remaining blocks to sync.
    */
  private def syncMissingBlocks(): URIO[ConfirmedBlockCallback, BlocksSyncOutcome] = {
    for {
      // Gets the number of the latest block processed by PRISM Node.
      maybeLastSyncedBlockNo <- keyValueRep.getInt(LAST_SYNCED_BLOCK_NO)
      // Calculates the next block based on the initial `blockNumberSyncStart` and the latest synced block.
      lastSyncedBlockNo = calculateLastSyncedBlockNo(
        maybeLastSyncedBlockNo,
        config.genesisBlockNumber
      )
      // Gets the latest block from the blocks database.
      latestBlock <- blocksRep.getLatestBlock
      // Calculates the latest confirmed block based on amount of required confirmations.
      lastConfirmedBlockNo = latestBlock.map(
        _.header.blockNo - config.blockConfirmationsToWait
      )
      syncStart = lastSyncedBlockNo + 1
      // Sync no more than `MAX_SYNC_BLOCKS` during one `syncMissingBlocks` iteration.
      syncEnd = lastConfirmedBlockNo.map(
        math.min(_, lastSyncedBlockNo + MAX_SYNC_BLOCKS)
      )
      // Sync all blocks with numbers from `syncStart` to `syncEnd`
      _ <- syncEnd.fold(_ => ZIO.unit, end => syncBlocksInRange(syncStart to end))
    } yield lastConfirmedBlockNo
      .flatMap(last =>
        syncEnd.map(end => if (last > end) BlocksSyncOutcome.MoreBlocksToSyncExist else BlocksSyncOutcome.NoMoreBlocks)
      )
      .getOrElse(BlocksSyncOutcome.NoMoreBlocks)
  }

  // Sync blocks in the given range.
  private def syncBlocksInRange(blockNos: Range): URIO[ConfirmedBlockCallback, Unit] = {
    if (blockNos.isEmpty) ZIO.unit
    else {
      // Sequentially sync blocks from the given range one by one.
      ZIO.foreachDiscard(blockNos)(blockNo => syncBlock(blockNo))
    }
  }

  // Sync block `blockNo` with internal state.
  private def syncBlock(blockNo: Int): URIO[ConfirmedBlockCallback, Unit] = {
    for {
      // Retrieve block header and the list of transactions in the block.
      block <- blocksRep.getFullBlock(blockNo)
      // Maybe in future we will add block handler here
      // Look over transactions in the block.
      _ <- block.fold(_ => ZIO.unit, filterNPushBlock)
    } yield ()
  }

  /** Filter out transactions in the `block` and push the block to the stream */
  private def filterNPushBlock(block: Block.Full): URIO[ConfirmedBlockCallback, Unit] = {
    val transactions: List[(TransactionId, proto.IrisBatch)] = for {
      // Iterate over transactions in the block.
      transaction <- block.transactions
      // Retrieve metadata from the transaction if it exists.
      metadata <- transaction.metadata
      // Parse metadata in accordance with the PRISM protocol if it's possible.
      irisBatch <- TransactionMetadata.fromTransactionMetadata(config.targetLedger, metadata)
      // Verify that operations related to the ledger protocol works on
      ops = irisBatch.operations.filter(op =>
        op.operation.createDid.forall(_.ledger == config.targetLedger.name) &&
          op.operation.updateDid.forall(_.ledger == config.targetLedger.name) &&
          op.operation.recoverDid.forall(_.ledger == config.targetLedger.name) &&
          op.operation.deactivateDid.forall(_.ledger == config.targetLedger.name)
      )
      nonEmptyBatch <-
        if (ops.nonEmpty) { Some(proto.IrisBatch(ops)) }
        else None
    } yield (transaction.id, nonEmptyBatch)

    val confirmedBlock =
      ConfirmedBlock(
        blockLevel = block.header.blockNo,
        blockTimestamp = block.header.time,
        transactions = transactions
      )

    for {
      cb <- ZIO.service[ConfirmedBlockCallback]
      // Trigger callback attached to ZStream on every block
      _ <- ZIO.succeed(cb(ZIO.succeed(Chunk(confirmedBlock))))
    } yield ()
  }

  private def calculateLastSyncedBlockNo(
      maybeLastSyncedBlockNo: Option[Int],
      blockNumberSyncStart: Int
  ): Int =
    math.max(maybeLastSyncedBlockNo.getOrElse(0), blockNumberSyncStart - 1)

}
