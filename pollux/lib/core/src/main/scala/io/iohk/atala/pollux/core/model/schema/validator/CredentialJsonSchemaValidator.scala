package io.iohk.atala.pollux.core.model.schema.validator

import com.networknt.schema.*
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError.*
import io.iohk.atala.pollux.core.model.schema.Schema
import io.iohk.atala.pollux.core.model.schema.common.JsonSchemaUtils
import zio.*

case class CredentialJsonSchemaValidator(schemaValidator: JsonSchema) extends CredentialSchemaValidator {
  override def validate(claims: String): IO[CredentialSchemaError, Unit] = {
    import scala.jdk.CollectionConverters.*
    for {
      // Convert claims to JsonNode
      jsonClaims <- JsonSchemaUtils.toJsonNode(claims)

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

object CredentialJsonSchemaValidator {
  def from(schema: Schema): IO[CredentialSchemaError, CredentialJsonSchemaValidator] = {
    for {
      jsonSchema <- JsonSchemaUtils.from(schema, IndexedSeq(SpecVersion.VersionFlag.V202012))
    } yield CredentialJsonSchemaValidator(jsonSchema)
  }
}
