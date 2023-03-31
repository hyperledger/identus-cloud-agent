package io.iohk.atala.connect.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.connect.controller.http.CreateConnectionRequest.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.util.UUID

case class CreateConnectionRequest(
    @description(annotations.label.description)
    @encodedExample(annotations.label.example)
    label: Option[String] = None
)

object CreateConnectionRequest {

  object annotations {
    object label
        extends Annotation[String](
          description = "A human readable alias for the connection.",
          example = "Peter"
        )
  }

  given encoder: JsonEncoder[CreateConnectionRequest] =
    DeriveJsonEncoder.gen[CreateConnectionRequest]

  given decoder: JsonDecoder[CreateConnectionRequest] =
    DeriveJsonDecoder.gen[CreateConnectionRequest]

  given schema: Schema[CreateConnectionRequest] = Schema.derived

}
