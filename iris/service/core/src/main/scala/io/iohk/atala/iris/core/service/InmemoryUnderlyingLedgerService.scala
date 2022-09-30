package io.iohk.atala.iris.core.service

import io.iohk.atala.iris.core.model.ledger.TransactionStatus.{InLedger, InMempool}
import io.iohk.atala.iris.core.model.ledger.{Funds, TransactionDetails, TransactionId}
import io.iohk.atala.iris.core.service.InmemoryUnderlyingLedgerService.{CardanoBlock, CardanoTransaction, Config}
import io.iohk.atala.iris.proto.dlt as proto
import io.iohk.atala.prism.crypto.Sha256
import zio.stm.*
import zio.*

object InmemoryUnderlyingLedgerService {
  case class Config(blockEvery: Duration, initialFunds: Funds, txFee: Funds)

  case class CardanoTransaction(operations: Seq[proto.IrisOperation]) {
    lazy val transactionId: TransactionId = {
      val objectBytes = proto.IrisBatch(operations).toByteArray
      val hash = Sha256.compute(objectBytes)
      TransactionId
        .from(hash.getValue)
        .getOrElse(throw new RuntimeException("Unexpected invalid hash"))
    }
  }

  case class CardanoBlock(txs: Seq[CardanoTransaction])

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
) extends UnderlyingLedgerService {

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

  private[service] def startBackgroundProcess(): UIO[Unit] = STM
    .atomically {
      for {
        // Craft a new block from mempool transactions
        txs <- mempoolRef.modify(old => (old, Vector.empty))
        _ <- blocksRef.update(_.appended(CardanoBlock(txs)))
      } yield ()
    }
    .repeat(Schedule.spaced(config.blockEvery))
    .fork
    .map(_ => ())
}
