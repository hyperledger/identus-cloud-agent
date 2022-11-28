package io.iohk.atala.pollux.schema.model

import sttp.tapir.Schema
import sttp.tapir.generic.auto.*
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

import java.time.ZonedDateTime

case class Proof(
    `type`: String,
    created: ZonedDateTime,
    verificationMethod: String,
    proofPurpose: String,
    proofValue: String,
    domain: Option[String]
)

object Proof {
  given encoder: zio.json.JsonEncoder[Proof] = DeriveJsonEncoder.gen[Proof]
  given decoder: zio.json.JsonDecoder[Proof] = DeriveJsonDecoder.gen[Proof]
  given schema: Schema[Proof] = Schema.derived
}
