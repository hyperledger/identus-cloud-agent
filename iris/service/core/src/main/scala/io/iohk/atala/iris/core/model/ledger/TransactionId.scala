package io.iohk.atala.iris.core.model.ledger

import com.typesafe.config.ConfigMemorySize
import io.iohk.atala.shared.{HashValueConfig, HashValueFrom}

import scala.collection.immutable.ArraySeq

case class TransactionId(hex: ArraySeq[Byte])

object TransactionId extends HashValueFrom[TransactionId] {

  override val config: HashValueConfig = HashValueConfig(
    ConfigMemorySize.ofBytes(32)
  )

  override protected def constructor(value: ArraySeq[Byte]): TransactionId =
    new TransactionId(value)
}
