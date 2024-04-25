package org.hyperledger.identus.pollux.core.model.schema.validator

sealed trait JsonSchemaError {
  def error: String
}

object JsonSchemaError {
  case class JsonSchemaParsingError(error: String) extends JsonSchemaError

  case class JsonValidationErrors(errors: Seq[String]) extends JsonSchemaError {
    def error: String = errors.mkString(";")
  }

  case class UnsupportedJsonSchemaSpecVersion(error: String) extends JsonSchemaError

  case class UnexpectedError(error: String) extends JsonSchemaError
}
