package org.hyperledger.identus.pollux.vc.jwt

import io.circe.*
import io.circe.syntax.*

case class MultiKey(
    publicKeyMultibase: Option[MultiBaseString] = None,
    secretKeyMultibase: Option[MultiBaseString] = None
) {
  val `type`: String = "Multikey"
  val `@context`: Set[String] = Set("https://w3id.org/security/multikey/v1")
}
object MultiKey {
  given multiKeyEncoder: Encoder[MultiKey] =
    (multiKey: MultiKey) =>
      Json
        .obj(
          ("@context", multiKey.`@context`.asJson),
          ("type", multiKey.`type`.asJson),
          ("publicKeyMultibase", multiKey.publicKeyMultibase.asJson),
          ("secretKeyMultibase", multiKey.secretKeyMultibase.asJson),
        )

  given multiKeyDecoder: Decoder[MultiKey] =
    (c: HCursor) =>
      for {
        publicKeyMultibase <- c.downField("publicKeyMultibase").as[Option[MultiBaseString]]
        secretKeyMultibase <- c.downField("secretKeyMultibase").as[Option[MultiBaseString]]
      } yield {
        MultiKey(
          publicKeyMultibase = publicKeyMultibase,
          secretKeyMultibase = secretKeyMultibase,
        )
      }

}
