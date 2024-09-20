package org.hyperledger.identus.oid4vci.http

import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class VerifiablePresentationRequest(
    responseMode: ResponseMode
)

object VerifiablePresentationRequest {
  given schema: Schema[VerifiablePresentationRequest] = Schema.derived
  given encoder: JsonEncoder[VerifiablePresentationRequest] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[VerifiablePresentationRequest] = DeriveJsonDecoder.gen
}

enum ResponseMode {
  case form_post
  case fragment
  case query
  case direct_post
  case `direct_post.jwt`
  case post
}

object ResponseMode {
  given schema: Schema[ResponseMode] = Schema.derivedEnumeration.defaultStringBased
  given encoder: JsonEncoder[ResponseMode] = JsonEncoder[String].contramap(_.toString)
  given decoder: JsonDecoder[ResponseMode] = JsonDecoder[String].mapOrFail { s =>
    ResponseMode.values.find(_.toString == s).toRight(s"Unknown ResponseMode: $s")
  }
}
