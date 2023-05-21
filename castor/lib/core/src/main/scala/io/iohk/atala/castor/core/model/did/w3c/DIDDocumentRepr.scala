package io.iohk.atala.castor.core.model.did.w3c

/** A projection of DIDDocument data model to W3C compliant DID representation */
final case class DIDDocumentRepr(
    id: String,
    controller: String,
    verificationMethod: Seq[PublicKeyRepr],
    authentication: Seq[PublicKeyReprOrRef],
    assertionMethod: Seq[PublicKeyReprOrRef],
    keyAgreement: Seq[PublicKeyReprOrRef],
    capabilityInvocation: Seq[PublicKeyReprOrRef],
    capabilityDelegation: Seq[PublicKeyReprOrRef],
    service: Seq[ServiceRepr],
    context: Seq[String]
)

type PublicKeyReprOrRef = PublicKeyRepr | String

final case class PublicKeyRepr(
    id: String,
    `type`: "EcdsaSecp256k1VerificationKey2019", // TODO: use JsonWebKey2020 (ATL-3788)
    controller: String,
    publicKeyJwk: PublicKeyJwk
)

final case class ServiceRepr(
    id: String,
    `type`: String,
    serviceEndpoint: Seq[String]
)

final case class PublicKeyJwk(kty: "EC", crv: String, x: String, y: String)
