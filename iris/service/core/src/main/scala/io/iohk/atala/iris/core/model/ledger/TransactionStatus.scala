package io.iohk.atala.iris.core.model.ledger

import enumeratum.{Enum, EnumEntry}
import enumeratum.EnumEntry.Snakecase
import scala.collection.immutable.ArraySeq

sealed trait TransactionStatus extends EnumEntry with Snakecase

object TransactionStatus extends Enum[TransactionStatus] {
  val values = ArraySeq(InWalletMempool, Submitted, Expired, InLedger)

  case object InWalletMempool extends TransactionStatus
  case object Submitted extends TransactionStatus
  case object Expired extends TransactionStatus
  case object InLedger extends TransactionStatus
}
