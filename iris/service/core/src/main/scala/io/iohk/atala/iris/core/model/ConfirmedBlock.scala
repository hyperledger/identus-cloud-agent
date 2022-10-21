package io.iohk.atala.iris.core.model

import io.iohk.atala.iris.proto.dlt as proto
import io.iohk.atala.iris.core.model.ledger.TransactionId

import java.time.Instant

case class ConfirmedBlock(
    blockLevel: Int,
    blockTimestamp: Instant,
    transactions: Seq[(TransactionId, proto.IrisBatch)]
)
