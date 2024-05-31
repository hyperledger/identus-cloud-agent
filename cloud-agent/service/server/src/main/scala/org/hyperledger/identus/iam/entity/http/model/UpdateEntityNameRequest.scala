package org.hyperledger.identus.iam.entity.http.model

import org.hyperledger.identus.api.http.Annotation
import org.hyperledger.identus.iam.entity.http.model.UpdateEntityNameRequest.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description
import sttp.tapir.Schema.annotations.encodedExample
import sttp.tapir.Schema.annotations.validate
import sttp.tapir.Schema.annotations.validateEach
import sttp.tapir.Validator
import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder

case class UpdateEntityNameRequest(
    @description(annotations.name.description)
    @encodedExample(annotations.name.example)
    @validate(annotations.name.validator)
    name: String
)

object UpdateEntityNameRequest {
  given encoder: JsonEncoder[UpdateEntityNameRequest] =
    DeriveJsonEncoder.gen[UpdateEntityNameRequest]

  given decoder: JsonDecoder[UpdateEntityNameRequest] =
    DeriveJsonDecoder.gen[UpdateEntityNameRequest]

  given schema: Schema[UpdateEntityNameRequest] = Schema.derived

  object annotations {
    object name
        extends Annotation[String](
          description = "New name of the entity",
          example = "John Doe",
          validator = Validator.all(Validator.nonEmptyString, Validator.maxLength(128))
        )
  }
}
