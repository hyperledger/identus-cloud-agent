package io.iohk.atala.castor.core.model.did

import java.time.Instant

final case class ConfirmedPublishedDIDOperation(
    anchoredAt: Instant,
    operation: PublishedDIDOperation
)
