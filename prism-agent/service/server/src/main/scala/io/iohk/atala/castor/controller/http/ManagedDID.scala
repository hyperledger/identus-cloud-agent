package io.iohk.atala.castor.controller.http

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}
import sttp.tapir.Schema

final case class ManagedDID(
    did: String,
    longFormDid: Option[String] = None,
    status: String
)

object ManagedDID {
  given encoder: JsonEncoder[ManagedDID] = DeriveJsonEncoder.gen[ManagedDID]
  given decoder: JsonDecoder[ManagedDID] = DeriveJsonDecoder.gen[ManagedDID]
  given schema: Schema[ManagedDID] = Schema.derived
}

final case class ManagedDIDPage(
    self: String,
    kind: String = "ManagedDIDPage",
    pageOf: String,
    next: Option[String] = None,
    previous: Option[String] = None,
    contents: Seq[ManagedDID]
)

object ManagedDIDPage {
  given encoder: JsonEncoder[ManagedDIDPage] = DeriveJsonEncoder.gen[ManagedDIDPage]
  given decoder: JsonDecoder[ManagedDIDPage] = DeriveJsonDecoder.gen[ManagedDIDPage]
  given schema: Schema[ManagedDIDPage] = Schema.derived
}
