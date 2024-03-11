package io.iohk.atala.pollux.core.model

import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.pollux.vc.jwt.StatusPurpose
import io.iohk.atala.shared.models.WalletId
import java.time.Instant
import java.util.UUID

final case class CredentialStatusList(
    id: UUID,
    walletId: WalletId,
    issuer: CanonicalPrismDID,
    issued: Instant,
    purpose: StatusPurpose,
    statusListCredential: String,
    size: Int,
    lastUsedIndex: Int,
    createdAt: Instant,
    updatedAt: Option[Instant]
)

case class CredInStatusList(
    id: UUID,
    issueCredentialRecordId: DidCommID,
    statusListIndex: Int,
    isCanceled: Boolean,
    isProcessed: Boolean,
)

case class CredentialStatusListWithCred(
    credentialStatusListId: UUID,
    issuer: CanonicalPrismDID,
    issued: Instant,
    purpose: StatusPurpose,
    walletId: WalletId,
    statusListCredential: String,
    size: Int,
    lastUsedIndex: Int,
    credentialInStatusListId: UUID,
    issueCredentialRecordId: DidCommID,
    statusListIndex: Int,
    isCanceled: Boolean,
    isProcessed: Boolean,
)

case class CredentialStatusListWithCreds(
    id: UUID,
    walletId: WalletId,
    issuer: CanonicalPrismDID,
    issued: Instant,
    purpose: StatusPurpose,
    statusListCredential: String,
    size: Int,
    lastUsedIndex: Int,
    credentials: Seq[CredInStatusList]
)
