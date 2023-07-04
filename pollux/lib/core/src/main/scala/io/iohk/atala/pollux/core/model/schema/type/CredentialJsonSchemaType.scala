package io.iohk.atala.pollux.core.model.schema.`type`

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.*
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError.*
import io.iohk.atala.pollux.core.model.schema.Schema
import io.iohk.atala.pollux.core.model.schema.validator.CredentialJsonSchemaValidator
import zio.*
import zio.json.*

object CredentialJsonSchemaType extends CredentialSchemaType {
  val VC_JSON_SCHEMA_URI = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json"

  override val `type`: String = VC_JSON_SCHEMA_URI

  override def toSchemaValidator(schema: Schema): IO[CredentialSchemaError, CredentialJsonSchemaValidator] = {
    for {
      mapper <- ZIO.attempt(new ObjectMapper()).mapError(t => UnexpectedError(t.getMessage))
      jsonSchemaNode <- ZIO
        .attempt(mapper.readTree(schema.toString()))
        .mapError(t => JsonSchemaParsingError(t.getMessage))
      specVersion <- ZIO
        .attempt(SpecVersionDetector.detect(jsonSchemaNode))
        .mapError(t => UnexpectedError(t.getMessage))
      _ <-
        if (specVersion != SpecVersion.VersionFlag.V202012)
          ZIO.fail(UnsupportedJsonSchemaSpecVersion(s"Version should be ${JsonMetaSchema.getV202012.getUri}"))
        else ZIO.unit
      factory <- ZIO
        .attempt(JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(specVersion)).objectMapper(mapper).build)
        .mapError(t => UnexpectedError(t.getMessage))
      jsonSchema <- ZIO.attempt(factory.getSchema(jsonSchemaNode)).mapError(t => UnexpectedError(t.getMessage))
    } yield CredentialJsonSchemaValidator(jsonSchema)
  }
}
