package org.hyperledger.identus.pollux.vc.jwt

import zio.json.{EncoderOps, JsonDecoder, JsonEncoder}
import zio.json.ast.{Json as ZioJson, JsonCursor}

case class MultiKey(
    publicKeyMultibase: Option[MultiBaseString] = None,
    secretKeyMultibase: Option[MultiBaseString] = None
) {
  val `type`: String = "Multikey"
  val `@context`: Set[String] = Set("https://w3id.org/security/multikey/v1")
}
object MultiKey {

  given JsonEncoder[MultiKey] = JsonEncoder[ZioJson].contramap { multiKey =>
    (for {
      context <- multiKey.`@context`.toJsonAST
      typ <- multiKey.`type`.toJsonAST
      publicKeyMultibase <- multiKey.publicKeyMultibase.toJsonAST
      secretKeyMultibase <- multiKey.secretKeyMultibase.toJsonAST
    } yield ZioJson.Obj(
      "@context" -> context,
      "type" -> typ,
      "publicKeyMultibase" -> publicKeyMultibase,
      "secretKeyMultibase" -> secretKeyMultibase,
    )).getOrElse(UnexpectedCodeExecutionPath)
  }

  given JsonDecoder[MultiKey] = JsonDecoder[ZioJson].mapOrFail { json =>
    for {
      publicKeyMultibase <- json.get(JsonCursor.field("publicKeyMultibase")).flatMap(_.as[Option[MultiBaseString]])
      secretKeyMultibase <- json.get(JsonCursor.field("secretKeyMultibase")).flatMap(_.as[Option[MultiBaseString]])
    } yield MultiKey(
      publicKeyMultibase = publicKeyMultibase,
      secretKeyMultibase = secretKeyMultibase,
    )
  }

}
