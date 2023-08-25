package io.iohk.atala.pollux.core.model.error

import io.iohk.atala.pollux.core.model.schema.validator.JsonSchemaError

sealed trait CredentialSchemaError {
  def userMessage: String
}

object CredentialSchemaError {
  case class SchemaError(schemaError: JsonSchemaError) extends CredentialSchemaError {
    def userMessage: String = schemaError.error
  }
  case class URISyntaxError(userMessage: String) extends CredentialSchemaError
  case class CredentialSchemaParsingError(userMessage: String) extends CredentialSchemaError
  case class UnsupportedCredentialSchemaType(userMessage: String) extends CredentialSchemaError
  case class UnexpectedError(userMessage: String) extends CredentialSchemaError
}
