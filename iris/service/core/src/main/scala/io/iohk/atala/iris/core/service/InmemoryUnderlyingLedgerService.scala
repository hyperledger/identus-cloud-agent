package io.iohk.atala.iris.core.service

import io.iohk.atala.iris.core.model.ledger.TransactionStatus.{InLedger, InMempool}
import io.iohk.atala.iris.core.model.ledger.*
import io.iohk.atala.iris.core.repository.ROBlocksRepository
import io.iohk.atala.iris.core.service.InmemoryUnderlyingLedgerService.{CardanoBlock, CardanoTransaction, Config}
import io.iohk.atala.iris.proto.dlt as proto
import io.iohk.atala.prism.crypto.Sha256
import io.circe.{Json, parser}
import zio.*
import zio.stm.*

import java.time.Instant
import java.util.concurrent.TimeUnit

object InmemoryUnderlyingLedgerService {
  case class Config(blockEvery: Duration, initialFunds: Funds, txFee: Funds, ledger: Ledger)

  case class CardanoTransaction(operations: Seq[proto.IrisOperation]) {
    lazy val transactionId: TransactionId = {
      val objectBytes = proto.IrisBatch(operations).toByteArray
      val hash = Sha256.compute(objectBytes)
      TransactionId
        .from(hash.getValue)
        .getOrElse(throw new RuntimeException("Unexpected invalid hash"))
    }
  }

  case class CardanoBlock(header: BlockHeader, txs: Seq[CardanoTransaction]) {
    def toBlockFull(ledger: Ledger): Block.Full = {
      Block.Full(
        header,
        txs.toList.map(tx =>
          Transaction(
            id = tx.transactionId,
            blockHash = header.hash,
            blockIndex = header.blockNo,
            metadata = Some(TransactionMetadata.toInmemoryTransactionMetadata(ledger, proto.IrisBatch(tx.operations)))
          )
        )
      )
    }
  }

  object CardanoBlock {
    def evalBlockHash(txs: Seq[CardanoTransaction], prevHash: Option[BlockHash]): BlockHash = {
      val bytes = prevHash.fold(Array.empty[Byte])(bh => bh.value.toArray)
      val hash = Sha256.compute(
        Array.concat(txs.map(_.transactionId.value.toArray).appended(bytes): _*)
      )
      BlockHash.from(hash.getValue).getOrElse(throw new RuntimeException("Unexpected invalid hash"))
    }
  }

  def layer(config: Config): ULayer[InmemoryUnderlyingLedgerService] = ZLayer.fromZIO {
    for {
      mempoolRef <- TRef.make(Vector[CardanoTransaction]()).commit
      blocksRef <- TRef.make(Vector[CardanoBlock]()).commit
      initialBalance <- TRef.make(config.initialFunds).commit
      srv = InmemoryUnderlyingLedgerService(config, mempoolRef, blocksRef, initialBalance)
      _ <- srv.startBackgroundProcess()
    } yield srv
  }
}

class InmemoryUnderlyingLedgerService(
    config: Config,
    mempoolRef: TRef[Vector[CardanoTransaction]],
    blocksRef: TRef[Vector[CardanoBlock]],
    balanceRef: TRef[Funds]
) extends UnderlyingLedgerService
    with ROBlocksRepository[Task] {

  override def publish(operations: Seq[proto.IrisOperation]): IO[LedgerError, Unit] =
    STM.atomically {
      for {
        curFunds <- balanceRef.get
        newFunds <- STM.cond(
          curFunds.lovelaces >= config.txFee.lovelaces,
          Funds(curFunds.lovelaces - config.txFee.lovelaces),
          LedgerError("Insufficient wallet balance")
        )
        _ <- balanceRef.set(newFunds)
        _ <- mempoolRef.update(_.appended(CardanoTransaction(operations)))
      } yield ()
    }

  override def getTransactionDetails(transactionId: TransactionId): IO[LedgerError, TransactionDetails] =
    STM.atomically {
      for {
        mempool <- mempoolRef.get
        blockchain <- blocksRef.get
        tdetails <- STM
          .fromOption {
            mempool
              .find(_.transactionId == transactionId)
              .map(_ => TransactionDetails(transactionId, InMempool))
          }
          .orElse {
            STM.fromOption {
              blockchain
                .find(block => block.txs.exists(t => t.transactionId == transactionId))
                .map(_ => TransactionDetails(transactionId, InLedger))
            }
          }
          .orElseFail(LedgerError(s"Couldn't find tx $transactionId"))
      } yield tdetails
    }

  override def deleteTransaction(transactionId: TransactionId): IO[LedgerError, Unit] = STM.atomically {
    for {
      mempool <- mempoolRef.get
      _ <- STM.cond(
        mempool.exists(_.transactionId == transactionId),
        (),
        LedgerError(s"Transaction $transactionId not found in the mempool")
      )
      _ <- mempoolRef.update(m => m.filter(_.transactionId != transactionId))
      _ <- balanceRef.update(b => Funds(b.lovelaces + config.txFee.lovelaces))
    } yield ()
  }

  override def getWalletBalance: IO[LedgerError, Funds] = balanceRef.get.commit

  def getMempool: UIO[List[CardanoTransaction]] = mempoolRef.get.commit.map(_.toList)

  def getBlocks: UIO[List[CardanoBlock]] = blocksRef.get.commit.map(_.toList)

  private[service] def startBackgroundProcess(): UIO[Unit] = (for {
    curTime <- Clock.currentTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli)
    _ <- STM
      .atomically {
        for {
          // Craft a new block from mempool transactions
          txs <- mempoolRef.modify(old => (old, Vector.empty))
          prevHash <- blocksRef.get.map(_.lastOption.map(_.header.hash))
          blockIdx <- blocksRef.get.map(_.size)
          blockHash = CardanoBlock.evalBlockHash(txs, prevHash)
          blockHeader = BlockHeader(blockHash, blockIdx, curTime, prevHash)
          _ <- blocksRef.update(_.appended(CardanoBlock(blockHeader, txs)))
        } yield ()
      }
  } yield ())
    .repeat(Schedule.spaced(config.blockEvery))
    .fork
    .map(_ => ())

  override def getFullBlock(blockNo: Int): Task[Either[BlockError.NotFound, Block.Full]] = STM.atomically {
    for {
      blocks <- blocksRef.get
      res =
        if (blockNo < blocks.size) {
          Right(blocks.drop(blockNo).head.toBlockFull(config.ledger))
        } else {
          Left(BlockError.NotFound(blockNo))
        }
    } yield res
  }

  override def getLatestBlock: Task[Either[BlockError.NoneAvailable.type, Block.Canonical]] = for {
    blocks <- blocksRef.get.commit
    res <-
      if (blocks.isEmpty) { ZIO.succeed(Left(BlockError.NoneAvailable)) }
      else ZIO.succeed(Right(Block.Canonical(blocks.last.header)))
  } yield res
}
