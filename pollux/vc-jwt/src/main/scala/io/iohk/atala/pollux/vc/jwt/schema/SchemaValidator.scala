package org.hyperledger.identus.pollux.vc.jwt.schema

import zio.prelude.Validation;
import io.circe.Json

trait SchemaValidator {
  def validate(payloadToValidate: Json): Validation[String, Json]
}
