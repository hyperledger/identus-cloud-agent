package org.hyperledger.identus.pollux.vc.jwt

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*

case class JsonWebKey(
    alg: Option[String] = Option.empty,
    crv: Option[String] = Option.empty,
    e: Option[String] = Option.empty,
    d: Option[String] = Option.empty,
    ext: Option[Boolean] = Option.empty,
    key_ops: Vector[String] = Vector.empty,
    kid: Option[String] = Option.empty,
    kty: String,
    n: Option[String] = Option.empty,
    use: Option[String] = Option.empty,
    x: Option[String] = Option.empty,
    y: Option[String] = Option.empty
)

object JsonWebKey {
  given jsonWebKeyEncoder: Encoder[JsonWebKey] = deriveEncoder[JsonWebKey]

  given jsonWebKeyDecoder: Decoder[JsonWebKey] = deriveDecoder[JsonWebKey]

}
