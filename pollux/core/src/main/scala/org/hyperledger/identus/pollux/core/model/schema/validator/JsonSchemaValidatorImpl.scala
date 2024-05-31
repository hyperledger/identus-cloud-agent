package org.hyperledger.identus.pollux.core.model.schema.validator

import com.networknt.schema.*
import org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError
import org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError.*
import org.hyperledger.identus.pollux.core.model.schema.Schema
import zio.*

case class JsonSchemaValidatorImpl(schemaValidator: JsonSchema) extends JsonSchemaValidator {
  override def validate(jsonString: String): IO[JsonSchemaError, Unit] = {
    import scala.jdk.CollectionConverters.*
    for {
      // Convert claims to JsonNode
      jsonClaims <- JsonSchemaUtils.toJsonNode(jsonString)

      // Validate claims JsonNode
      validationMessages <- ZIO
        .attempt(schemaValidator.validate(jsonClaims).asScala.toSeq)
        .mapError(t => JsonSchemaError.JsonValidationErrors(Seq(t.getMessage)))

      validationResult <-
        if (validationMessages.isEmpty) ZIO.unit
        else ZIO.fail(JsonSchemaError.JsonValidationErrors(validationMessages.map(_.getMessage)))
    } yield validationResult
  }

}

object JsonSchemaValidatorImpl {
  def from(schema: Schema): IO[JsonSchemaError, JsonSchemaValidatorImpl] = {
    for {
      jsonSchema <- JsonSchemaUtils.from(schema, IndexedSeq(SpecVersion.VersionFlag.V202012))
    } yield JsonSchemaValidatorImpl(jsonSchema)
  }
}
