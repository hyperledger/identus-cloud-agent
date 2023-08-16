package io.iohk.atala.multitenancy.core.model

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.models.WalletId

import java.time.Instant

/** @param createdAt
  * @param updatedAt
  * @param walletId
  * @param did
  */
case class DidWalletMappingRecord(
    did: DidId,
    walletId: WalletId,
    createdAt: Instant,
    updatedAt: Option[Instant]
)
