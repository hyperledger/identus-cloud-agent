package io.iohk.atala.iris.core.model.ledger

import java.time.Instant

case class BlockHeader(
    hash: BlockHash,
    blockNo: Int,
    time: Instant,
    previousBlockHash: Option[BlockHash]
)
