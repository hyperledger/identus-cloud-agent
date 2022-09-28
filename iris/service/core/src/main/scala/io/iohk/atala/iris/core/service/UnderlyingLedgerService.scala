package io.iohk.atala.iris.core.service

import io.iohk.atala.iris.proto.{service => proto}
import io.iohk.atala.iris.core.model.IrisOperation
import io.iohk.atala.iris.core.model.ledger.{Funds, TransactionDetails, TransactionId}
import zio.{IO, UIO}

case class LedgerError(msg: String) extends RuntimeException(msg)

trait UnderlyingLedgerService {
//  def getType: Ledger

  def publish(operations: Seq[proto.IrisOperation]): IO[LedgerError, Unit]

  def getTransactionDetails(transactionId: TransactionId): IO[LedgerError, TransactionDetails]

  def deleteTransaction(transactionId: TransactionId): IO[LedgerError, Unit]

  def getWalletBalance: IO[LedgerError, Funds]
}
