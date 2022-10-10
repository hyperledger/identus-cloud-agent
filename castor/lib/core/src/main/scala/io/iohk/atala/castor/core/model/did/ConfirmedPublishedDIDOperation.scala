package io.iohk.atala.castor.core.model.did

import java.time.Instant

final case class ConfirmedPublishedDIDOperation(
    operation: PublishedDIDOperation,
    anchoredAt: Instant,
    blockNumber: Int,
    blockIndex: Int
)
