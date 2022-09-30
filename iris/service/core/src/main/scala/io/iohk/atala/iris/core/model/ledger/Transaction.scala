package io.iohk.atala.iris.core.model.ledger

case class Transaction(
    id: TransactionId,
    blockHash: BlockHash,
    blockIndex: Int,
    metadata: Option[TransactionMetadata]
)
