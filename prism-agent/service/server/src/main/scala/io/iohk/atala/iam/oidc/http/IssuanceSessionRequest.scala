package io.iohk.atala.iam.oidc.http

import sttp.tapir.Schema
import sttp.tapir.json.zio.schemaForZioJsonValue
import zio.json.ast.Json
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class IssuanceSessionRequest(
    nonce: String,
    issuableCredentials: Seq[IssuableCredential],
    isPreAuthorized: Boolean,
    did: Option[String],
    issuerDid: Option[String],
    userPin: Option[String]
)

object IssuanceSessionRequest {
  given schema: Schema[IssuanceSessionRequest] = Schema.derived
  given encoder: JsonEncoder[IssuanceSessionRequest] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[IssuanceSessionRequest] = DeriveJsonDecoder.gen
}

case class IssuableCredential(`type`: String, claims: Json)

object IssuableCredential {
  given schema: Schema[IssuableCredential] = Schema.derived
  given encoder: JsonEncoder[IssuableCredential] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[IssuableCredential] = DeriveJsonDecoder.gen
}
