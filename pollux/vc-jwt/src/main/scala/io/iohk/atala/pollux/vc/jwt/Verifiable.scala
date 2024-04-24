package org.hyperledger.identus.pollux.vc.jwt

import io.circe
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*

import scala.annotation.unused

trait Verifiable(@unused proof: JwtProof)

// JwtProof2020 is not following the spec
case class JwtProof(`type`: String = "JwtProof2020", jwt: JWT)

object JwtProof {

  object Implicits {
    implicit val proofEncoder: Encoder[JwtProof] =
      (proof: JwtProof) =>
        Json
          .obj(
            ("type", proof.`type`.asJson),
            ("jwt", proof.jwt.value.asJson)
          )
    implicit val proofDecoder: Decoder[JwtProof] =
      (c: HCursor) =>
        for {
          `type` <- c.downField("type").as[String]
          jwt <- c.downField("jwt").as[String]
        } yield {
          JwtProof(
            `type` = `type`,
            jwt = JWT(jwt)
          )
        }

  }
}
