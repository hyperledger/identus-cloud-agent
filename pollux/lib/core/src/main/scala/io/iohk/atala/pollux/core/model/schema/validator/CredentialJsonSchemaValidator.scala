package io.iohk.atala.pollux.core.model.schema.validator

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.*
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError.*
import zio.*

case class CredentialJsonSchemaValidator(schemaValidator: JsonSchema) extends CredentialSchemaValidator {
  override def validate(claims: String): IO[CredentialSchemaError, Unit] = {
    import scala.jdk.CollectionConverters.*
    for {
      mapper <- ZIO.attempt(new ObjectMapper()).mapError(t => UnexpectedError(t.getMessage))

      // Convert claims to JsonNode
      jsonClaims <- ZIO
        .attempt(mapper.readTree(claims))
        .mapError(t => ClaimsParsingError(t.getMessage))

      // Validate claims JsonNode
      validationMessages <- ZIO
        .attempt(schemaValidator.validate(jsonClaims).asScala.toSeq)
        .mapError(t => ClaimsValidationError(Seq(t.getMessage)))

      validationResult <-
        if (validationMessages.isEmpty) ZIO.unit
        else ZIO.fail(ClaimsValidationError(validationMessages.map(_.getMessage)))
    } yield validationResult
  }

}
