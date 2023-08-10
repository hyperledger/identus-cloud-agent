package io.iohk.atala.iam.entity.http.model

import io.iohk.atala.api.http.Annotation
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample, validate, validateEach}
import sttp.tapir.Validator.*
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import UpdateEntityNameRequest.annotations

import java.time.Instant
import java.util.UUID

case class UpdateEntityNameRequest(
    @description(annotations.name.description)
    @encodedExample(annotations.name.example)
    @validate(nonEmptyString)
    name: String
)

object UpdateEntityNameRequest {
  given encoder: JsonEncoder[UpdateEntityNameRequest] =
    DeriveJsonEncoder.gen[UpdateEntityNameRequest]

  given decoder: JsonDecoder[UpdateEntityNameRequest] =
    DeriveJsonDecoder.gen[UpdateEntityNameRequest]

  given schema: Schema[UpdateEntityNameRequest] = Schema.derived

  object annotations {
    object name extends Annotation[String](description = "New name of the entity", example = "John Doe")
  }
}
