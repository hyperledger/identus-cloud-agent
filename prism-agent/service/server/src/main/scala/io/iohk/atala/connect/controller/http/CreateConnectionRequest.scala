package io.iohk.atala.connect.controller.http

import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class CreateConnectionRequest(
    label: Option[String] = None
)

object CreateConnectionRequest {

  given encoder: JsonEncoder[CreateConnectionRequest] =
    DeriveJsonEncoder.gen[CreateConnectionRequest]

  given decoder: JsonDecoder[CreateConnectionRequest] =
    DeriveJsonDecoder.gen[CreateConnectionRequest]

  given schema: Schema[CreateConnectionRequest] = Schema.derived

}
