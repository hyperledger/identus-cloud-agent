package io.iohk.atala.castor.core.model.did.repr

final case class PublicKeyRepr(
    id: String,
    `type`: String,
    controller: String,
    jsonWebKey2020: Option[JsonWebKey2020Repr]
)

final case class JsonWebKey2020Repr(
    publicKeyJwk: PublicKeyJwkRepr
)

final case class PublicKeyJwkRepr(
    crv: String,
    x: String,
    y: String,
    kty: String,
    kid: Option[String]
)
