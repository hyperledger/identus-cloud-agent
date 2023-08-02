package io.iohk.atala.pollux.core.model.schema.validator

import com.fasterxml.jackson.databind.JsonNode
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import zio.*

trait CredentialSchemaValidator {
  def validate(claims: String): IO[CredentialSchemaError, Unit]

  def validate(claimsJsonNode: JsonNode): IO[CredentialSchemaError, Unit] = {
    validate(claimsJsonNode.toString)
  }
}
