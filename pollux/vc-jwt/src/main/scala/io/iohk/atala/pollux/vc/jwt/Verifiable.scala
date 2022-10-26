package io.iohk.atala.pollux.vc.jwt

import io.circe
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*

trait Verifiable(proof: Proof)

case class Proof(`type`: String, jwt: JWT)

object Proof {

  object Implicits {
    implicit val proofEncoder: Encoder[Proof] =
      (proof: Proof) =>
        Json
          .obj(
            ("type", proof.`type`.asJson),
            ("jwt", proof.jwt.value.asJson)
          )
    implicit val proofDecoder: Decoder[Proof] =
      (c: HCursor) =>
        for {
          `type` <- c.downField("type").as[String]
          jwt <- c.downField("jwt").as[String]
        } yield {
          Proof(
            `type` = `type`,
            jwt = JWT(jwt)
          )
        }

  }
}
