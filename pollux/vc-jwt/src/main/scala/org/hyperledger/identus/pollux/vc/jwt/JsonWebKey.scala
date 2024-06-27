package org.hyperledger.identus.pollux.vc.jwt

import io.circe.*
import io.circe.generic.semiauto.*
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

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
  given jsonWebKeyEncoderCirce: Encoder[JsonWebKey] = deriveEncoder[JsonWebKey]

  given jsonWebKeyDecoderCirce: Decoder[JsonWebKey] = deriveDecoder[JsonWebKey]

  given jsonWebKeyEncoderCirceZioJson: JsonEncoder[JsonWebKey] = DeriveJsonEncoder.gen[JsonWebKey]

  given jsonWebKeyDecoderCirceZioJson: JsonDecoder[JsonWebKey] = DeriveJsonDecoder.gen[JsonWebKey]

}
