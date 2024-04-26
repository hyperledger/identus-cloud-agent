package org.hyperledger.identus.pollux.core.model.error

import org.hyperledger.identus.pollux.core.model.schema.validator.JsonSchemaError

sealed trait CredentialSchemaError {
  def message: String
}

object CredentialSchemaError {
  case class SchemaError(schemaError: JsonSchemaError) extends CredentialSchemaError {
    def message: String = schemaError.error
  }
  case class URISyntaxError(message: String) extends CredentialSchemaError
  case class CredentialSchemaParsingError(message: String) extends CredentialSchemaError
  case class UnsupportedCredentialSchemaType(message: String) extends CredentialSchemaError
  case class UnexpectedError(message: String) extends CredentialSchemaError
}
