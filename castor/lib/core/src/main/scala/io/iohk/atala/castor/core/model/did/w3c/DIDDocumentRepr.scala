package io.iohk.atala.castor.core.model.did.w3c

import io.iohk.atala.castor.core.model.did.{DID, DIDUrl}

/** A projection of DIDDocument data model to W3C compliant DID representation */
final case class DIDDocumentRepr(
    id: String,
    controller: String,
    verificationMethod: Seq[PublicKeyRepr],
    authentication: Seq[PublicKeyRepr],
    assertionMethod: Seq[PublicKeyRepr],
    keyAgreement: Seq[PublicKeyRepr],
    capabilityInvocation: Seq[PublicKeyRepr],
    service: Seq[ServiceRepr]
)

final case class PublicKeyRepr(
    id: String,
    `type`: "EcdsaSecp256k1VerificationKey2019",
    controller: String,
    publicKeyBase58: Option[String]
)

final case class ServiceRepr(
    id: String,
    `type`: String,
    serviceEndpoint: String
)
