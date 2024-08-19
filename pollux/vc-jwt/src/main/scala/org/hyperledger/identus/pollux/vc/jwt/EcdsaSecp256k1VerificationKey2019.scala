package org.hyperledger.identus.pollux.vc.jwt

import io.circe.*
import io.circe.syntax.*

import java.time.{Instant, ZoneOffset}

case class EcdsaSecp256k1VerificationKey2019(
    publicKeyJwk: JsonWebKey,
    id: Option[String] = None,
    controller: Option[String] = None,
    expires: Option[Instant] = None
) {
  val `type`: String = "EcdsaSecp256k1VerificationKey2019"
  val `@context`: Set[String] =
    Set("https://w3id.org/security/v1")
}

object EcdsaSecp256k1VerificationKey2019 {
  given ecdsaSecp256k1VerificationKey2019Encoder: Encoder[EcdsaSecp256k1VerificationKey2019] =
    (key: EcdsaSecp256k1VerificationKey2019) =>
      Json
        .obj(
          ("@context", key.`@context`.asJson),
          ("type", key.`type`.asJson),
          ("id", key.id.asJson),
          ("controller", key.controller.asJson),
          ("publicKeyJwk", key.publicKeyJwk.asJson.dropNullValues),
          ("expires", key.expires.map(_.atOffset(ZoneOffset.UTC)).asJson)
        )

  given ecdsaSecp256k1VerificationKey2019Decoder: Decoder[EcdsaSecp256k1VerificationKey2019] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[Option[String]]
        `type` <- c.downField("type").as[String]
        controller <- c.downField("controller").as[Option[String]]
        publicKeyJwk <- c.downField("publicKeyJwk").as[JsonWebKey]
        expires <- c.downField("expires").as[Option[Instant]]
      } yield {
        EcdsaSecp256k1VerificationKey2019(
          id = id,
          publicKeyJwk = publicKeyJwk,
          controller = controller,
          expires = expires
        )
      }

}
