package io.iohk.atala.pollux.vc.jwt

import io.circe
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*

trait Verifiable(proof: Proof)

case class Proof(`type`: String, other: Json)

object Proof {

  object Implicits {
    implicit val proofEncoder: Encoder[Proof] =
      (proof: Proof) => proof.other.deepMerge(Map("type" -> proof.`type`).asJson)
    implicit val proofDecoder: Decoder[Proof] =
      (c: HCursor) =>
        for {
          `type` <- c.downField("type").as[String]
          other <- c.downField("type").delete.up.as[Json]
        } yield {
          Proof(
            `type` = `type`,
            other = other
          )
        }

  }
}
