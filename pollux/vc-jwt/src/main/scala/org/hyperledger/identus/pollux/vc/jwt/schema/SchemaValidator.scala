package org.hyperledger.identus.pollux.vc.jwt.schema

import io.circe.Json
import zio.prelude.Validation

trait SchemaValidator {
  def validate(payloadToValidate: Json): Validation[String, Json]
}
