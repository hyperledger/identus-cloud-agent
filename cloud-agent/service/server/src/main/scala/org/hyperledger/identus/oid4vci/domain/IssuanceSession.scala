package org.hyperledger.identus.oid4vci.domain

import org.hyperledger.identus.castor.core.model.did.CanonicalPrismDID
import org.hyperledger.identus.castor.core.model.did.DID
import org.hyperledger.identus.castor.core.model.did.PrismDID

import java.util.UUID

case class IssuanceSession(
    id: UUID,
    nonce: String,
    issuerState: String,
    schemaId: Option[String],
    claims: zio.json.ast.Json,
    subjectDid: Option[DID],
    issuingDid: PrismDID,
)
