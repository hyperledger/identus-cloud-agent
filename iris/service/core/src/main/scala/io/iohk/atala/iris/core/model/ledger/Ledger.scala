package io.iohk.atala.iris.core.model.ledger

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.ArraySeq

sealed trait Ledger extends EnumEntry

object Ledger extends Enum[Ledger] {
  val values = ArraySeq(InMemory, CardanoMainnet, CardanoTestnet)

  case object InMemory extends Ledger
  case object CardanoMainnet extends Ledger
  case object CardanoTestnet extends Ledger
}
