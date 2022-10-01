package io.iohk.atala.iris.core.model.ledger

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.ArraySeq

case class Ledger(name: String)

object Ledger{
  val InMemory: Ledger = Ledger("in-memory")
  val Mainnet: Ledger = Ledger("mainnet")
}
