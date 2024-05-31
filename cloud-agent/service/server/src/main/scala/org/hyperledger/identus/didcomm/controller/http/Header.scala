package org.hyperledger.identus.didcomm.controller.http

import sttp.tapir.Schema
import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder

case class Header(kid: String)

object Header {
  given encoder: JsonEncoder[Header] = DeriveJsonEncoder.gen[Header]
  given decoder: JsonDecoder[Header] = DeriveJsonDecoder.gen[Header]
  given schema: Schema[Header] = Schema.derived
}
