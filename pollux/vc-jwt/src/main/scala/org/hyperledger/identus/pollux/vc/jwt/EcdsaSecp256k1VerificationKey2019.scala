package org.hyperledger.identus.pollux.vc.jwt

import zio.json.{EncoderOps, JsonDecoder, JsonEncoder}
import zio.json.ast.{Json, JsonCursor}

import java.time.{Instant, ZoneOffset}

case class EcdsaSecp256k1VerificationKey2019(
    publicKeyJwk: JsonWebKey,
    id: Option[String] = None,
    controller: Option[String] = None,
    expires: Option[Instant] = None
) {
  val `type`: String = "EcdsaSecp256k1VerificationKey2019"
  val `@context`: Set[String] = Set("https://w3id.org/security/v1")
}

object EcdsaSecp256k1VerificationKey2019 {
  given JsonEncoder[EcdsaSecp256k1VerificationKey2019] = JsonEncoder[Json].contramap { key =>
    (for {
      context <- key.`@context`.toJsonAST
      typ <- key.`type`.toJsonAST
      id <- key.id.toJsonAST
      controller <- key.controller.toJsonAST
      publicKeyJwk <- key.publicKeyJwk.toJsonAST
      expires <- key.expires.map(_.atOffset(ZoneOffset.UTC)).toJsonAST
    } yield Json.Obj(
      "@context" -> context,
      "type" -> typ,
      "id" -> id,
      "controller" -> controller,
      "publicKeyJwk" -> publicKeyJwk,
      "expires" -> expires
    )).getOrElse(UnexpectedCodeExecutionPath)
  }

  given JsonDecoder[EcdsaSecp256k1VerificationKey2019] = JsonDecoder[Json].mapOrFail { json =>
    for {
      id <- json.get(JsonCursor.field("id")).flatMap(_.as[Option[String]])
      controller <- json.get(JsonCursor.field("controller")).flatMap(_.as[Option[String]])
      publicKeyJwk <- json.get(JsonCursor.field("publicKeyJwk")).flatMap(_.as[JsonWebKey])
      expires <- json.get(JsonCursor.field("expires")).flatMap(_.as[Option[Instant]])
    } yield EcdsaSecp256k1VerificationKey2019(
      id = id,
      publicKeyJwk = publicKeyJwk,
      controller = controller,
      expires = expires
    )
  }

}
