package io.iohk.atala.iris.core.service

import io.iohk.atala.iris.core.model.ledger.TransactionStatus.{InWalletMempool, InLedger}
import io.iohk.atala.iris.core.model.ledger.{Funds, TransactionDetails, TransactionId}
import io.iohk.atala.iris.core.service.InmemoryUnderlyingLedgerService.{CardanoBlock, CardanoTransaction, Config}
import io.iohk.atala.iris.proto.service.IrisOperation
import zio.{Duration as ZDuration, *}
import zio.stm.*
import io.iohk.atala.iris.proto.dlt_operations as proto_dlt
import io.iohk.atala.iris.proto.service as proto_service
import io.iohk.atala.prism.crypto.Sha256

import scala.concurrent.duration.Duration

object InmemoryUnderlyingLedgerService {
  case class Config(blockEvery: Duration, initialFunds: Funds, txFee: Funds)

  case class CardanoTransaction(operations: Seq[proto_service.IrisOperation]) {
    lazy val transactionId: TransactionId = {
      val objectBytes = proto_dlt.AtalaObject(operations).toByteArray
      val hash = Sha256.compute(objectBytes)
      TransactionId
        .from(hash.getValue)
        .getOrElse(throw new RuntimeException("Unexpected invalid hash"))
    }
  }

  case class CardanoBlock(txs: List[CardanoTransaction])

  def layer(config: Config): ULayer[UnderlyingLedgerService] = ZLayer.fromZIO {
    for {
      mempoolRef <- TRef.make(List[CardanoTransaction]()).commit
      blocksRef <- TRef.make(List[CardanoBlock]()).commit
      initialBalance <- TRef.make(config.initialFunds).commit
      srv = InmemoryUnderlyingLedgerService(config, mempoolRef, blocksRef, initialBalance)
      _ <- srv.startBackgroundProcess()
    } yield srv
  }
}

class InmemoryUnderlyingLedgerService(
                                       config: Config,
                                       mempoolRef: TRef[List[CardanoTransaction]],
                                       blocksRef: TRef[List[CardanoBlock]],
                                       balanceRef: TRef[Funds]
                                     ) extends UnderlyingLedgerService {

  def startBackgroundProcess(): UIO[Unit] = STM.atomically {
    for {
      // Craft a new block from mempool transactions
      txs <- mempoolRef.modify(old => (old, List.empty))
      _ <- blocksRef.update(CardanoBlock(txs) :: _)
    } yield ()
  }
    .repeat(Schedule.spaced(ZDuration.fromScala(config.blockEvery)))
    .fork.map(_ => ())

  override def publish(operations: Seq[proto_service.IrisOperation]): IO[LedgerError, Unit] =
    STM.atomically {
      for {
        curFunds <- balanceRef.get
        newFunds <- STM.cond(
          curFunds.lovelaces >= config.txFee.lovelaces,
          Funds(curFunds.lovelaces - config.txFee.lovelaces),
          LedgerError("Insufficient wallet balance")
        )
        _ <- balanceRef.set(newFunds)
        _ <- mempoolRef.update(CardanoTransaction(operations) :: _)
      } yield ()
    }

  override def getTransactionDetails(transactionId: TransactionId): IO[LedgerError, TransactionDetails] =
    STM.atomically {
      for {
        mempool <- mempoolRef.get
        blockchain <- blocksRef.get
        tdetails <- STM.fromOption {
          mempool.find(_.transactionId == transactionId)
            .map(_ => TransactionDetails(transactionId, InWalletMempool))
        }.orElse {
          STM.fromOption {
            blockchain.find(block => block.txs.exists(t => t.transactionId == transactionId))
              .map(_ => TransactionDetails(transactionId, InLedger))
          }
        }
          .orElseFail(LedgerError(s"Couldn't find tx $transactionId"))
      } yield tdetails
    }

  override def deleteTransaction(transactionId: TransactionId): IO[LedgerError, Unit] = STM.atomically {
    for {
      mempool <- mempoolRef.get
      _ <- STM.cond(mempool.exists(_.transactionId == transactionId),
        (),
        LedgerError(s"Transaction $transactionId not found in the mempool")
      )
      _ <- mempoolRef.update(m => m.filter {
        _.transactionId != transactionId
      })
      _ <- balanceRef.update(b => Funds(b.lovelaces + config.txFee.lovelaces))
    } yield ()
  }

  override def getWalletBalance: IO[LedgerError, Funds] = balanceRef.get.commit
}
