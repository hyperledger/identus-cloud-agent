package org.hyperledger.identus.pollux.core.model.schema.validator

import com.fasterxml.jackson.databind.JsonNode
import zio.*

trait JsonSchemaValidator {
  def validate(claims: String): IO[JsonSchemaError, Unit]

  def validate(claimsJsonNode: JsonNode): IO[JsonSchemaError, Unit] = {
    validate(claimsJsonNode.toString)
  }
}
