package org.hyperledger.identus.pollux.vc.jwt

import io.circe.*
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import scala.annotation.unused

trait Verifiable(@unused proof: JwtProof)

// JwtProof2020 is not following the spec
case class JwtProof(`type`: String = "JwtProof2020", jwt: JWT)

object JwtProof {

  object Implicits {
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

  given JsonEncoder[JwtProof] = DeriveJsonEncoder.gen
  given JsonDecoder[JwtProof] = DeriveJsonDecoder.gen
}
