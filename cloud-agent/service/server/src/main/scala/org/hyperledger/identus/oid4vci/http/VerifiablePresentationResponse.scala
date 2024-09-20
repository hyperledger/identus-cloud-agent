package org.hyperledger.identus.oid4vci.http

import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class VerifiablePresentationResponse(
    autorizationRequest: AuthorizationRequest
)

object VerifiablePresentationResponse {
  given schema: Schema[VerifiablePresentationResponse] = Schema.derived
  given encoder: JsonEncoder[VerifiablePresentationResponse] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[VerifiablePresentationResponse] = DeriveJsonDecoder.gen
}
