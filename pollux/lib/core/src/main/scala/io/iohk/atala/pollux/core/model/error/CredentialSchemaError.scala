package io.iohk.atala.pollux.core.model.error

sealed trait CredentialSchemaError {
  def userMessage: String
}

object CredentialSchemaError {
  case class URISyntaxError(userMessage: String) extends CredentialSchemaError
  case class CredentialSchemaParsingError(userMessage: String) extends CredentialSchemaError
  case class UnsupportedCredentialSchemaType(userMessage: String) extends CredentialSchemaError
  case class JsonSchemaParsingError(userMessage: String) extends CredentialSchemaError
  case class UnsupportedJsonSchemaSpecVersion(userMessage: String) extends CredentialSchemaError
  case class ClaimsParsingError(userMessage: String) extends CredentialSchemaError
  case class ClaimsValidationError(errors: Seq[String]) extends CredentialSchemaError {
    def userMessage: String = errors.mkString(";")
  }
  case class UnexpectedError(userMessage: String) extends CredentialSchemaError
}
