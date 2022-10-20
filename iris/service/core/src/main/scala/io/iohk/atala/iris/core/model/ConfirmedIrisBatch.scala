package io.iohk.atala.iris.core.model

import io.iohk.atala.iris.core.model.ledger.TransactionId
import io.iohk.atala.iris.proto.dlt as proto

import java.time.Instant

case class ConfirmedIrisBatch(
    blockLevel: Int,
    blockTimestamp: Instant,
    transactionSeqId: Int,
    transactionId: TransactionId,
    batch: proto.IrisBatch
)
