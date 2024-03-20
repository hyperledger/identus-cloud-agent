package io.iohk.atala.iam.oidc.domain

import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.iam.oidc.http.IssuableCredential

case class IssuanceSession(
    nonce: String,
    issuableCredentials: Seq[IssuableCredential],
    isPreAuthorized: Boolean,
    did: Option[String],
    issuerDid: CanonicalPrismDID,
    userPin: Option[String]
)
