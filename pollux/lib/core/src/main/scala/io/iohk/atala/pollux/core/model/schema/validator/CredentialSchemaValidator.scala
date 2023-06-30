package io.iohk.atala.pollux.core.model.schema.validator

import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import zio.*

trait CredentialSchemaValidator {
  def validate(claims: String): IO[CredentialSchemaError, Unit]
}
