package io.iohk.atala.pollux.vc.jwt.schema

import io.circe
import io.circe.Json
import zio.prelude.*

class PlaceholderSchemaValidator extends SchemaValidator {
  override def validate(payloadToValidate: Json): Validation[String, Json] = Validation.succeed(payloadToValidate)
}

object PlaceholderSchemaValidator {
  def fromSchema(schema: Json): Validation[String, PlaceholderSchemaValidator] =
    Validation.succeed(PlaceholderSchemaValidator())
}
