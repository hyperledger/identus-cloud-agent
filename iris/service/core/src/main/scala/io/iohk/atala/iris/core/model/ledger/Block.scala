package io.iohk.atala.iris.core.model.ledger

sealed trait Block extends Product with Serializable {
  def header: BlockHeader
}

object Block {
  final case class Canonical(override val header: BlockHeader) extends Block

  final case class Full(
      override val header: BlockHeader,
      transactions: List[Transaction]
  ) extends Block {
    def toCanonical: Canonical = Canonical(header)
  }
}
