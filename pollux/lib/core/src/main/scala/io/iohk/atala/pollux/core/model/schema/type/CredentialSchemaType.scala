package io.iohk.atala.pollux.core.model.schema.`type`

import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.schema.Schema
import io.iohk.atala.pollux.core.model.schema.validator.CredentialSchemaValidator
import zio.IO

trait CredentialSchemaType {
  val `type`: String

  def toSchemaValidator(schema: Schema): IO[CredentialSchemaError, CredentialSchemaValidator]

  def validateClaims(schema: Schema, claims: String): IO[CredentialSchemaError, Unit] = {
    for {
      schemaValidator <- toSchemaValidator(schema)
      validatedClaims <- schemaValidator.validate(claims)
    } yield validatedClaims
  }
}
