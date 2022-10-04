package io.iohk.atala.castor.core.model.did.repr

import io.iohk.atala.castor.core.model.did.{DID, DIDUrl}

/** A projection of DIDDocument data model to W3C compliant DID representation */
final case class DIDDocumentRepr(
    id: String,
    controller: String,
    verificationMethod: Seq[PublicKeyRepr],
    authentication: Seq[PublicKeyRepr | String],
    assertionMethod: Seq[PublicKeyRepr | String],
    keyAgreement: Seq[PublicKeyRepr | String],
    capabilityInvocation: Seq[PublicKeyRepr | String],
    service: Seq[ServiceRepr]
)
