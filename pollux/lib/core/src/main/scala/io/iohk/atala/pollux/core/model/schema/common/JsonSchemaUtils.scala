package io.iohk.atala.pollux.core.model.schema.common

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.networknt.schema.*
import com.networknt.schema.SpecVersion.VersionFlag
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError.*
import zio.*
import zio.json.ast.Json

object JsonSchemaUtils {
  def jsonSchema(schema: String, supportedVersions: IndexedSeq[VersionFlag] = IndexedSeq.empty): IO[CredentialSchemaError, JsonSchema] = {
    for {
      jsonSchemaNode <- toJsonNode(schema)
      specVersion <- ZIO
        .attempt(SpecVersionDetector.detect(jsonSchemaNode))
        .mapError(t => UnexpectedError(t.getMessage))
      _ <-
        if (supportedVersions.nonEmpty && !supportedVersions.contains(specVersion))
          ZIO.fail(UnsupportedJsonSchemaSpecVersion(s"Unsupported JsonSchemaVersion. Current:$specVersion ExpectedOneOf:${supportedVersions.map(_.getId)}"))
        else ZIO.unit
      mapper <- ZIO.attempt(new ObjectMapper()).mapError(t => UnexpectedError(t.getMessage))
      factory <- ZIO
        .attempt(JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(specVersion)).objectMapper(mapper).build)
        .mapError(t => UnexpectedError(t.getMessage))
      jsonSchema <- ZIO.attempt(factory.getSchema(jsonSchemaNode)).mapError(t => UnexpectedError(t.getMessage))
    } yield jsonSchema
  }

  def from(schema: Json, supportedVersions: IndexedSeq[VersionFlag] = IndexedSeq.empty): IO[CredentialSchemaError, JsonSchema] = {
    jsonSchema(schema.toString(), supportedVersions)
  }

  def toJsonNode(json: Json): IO[CredentialSchemaError, JsonNode] = {
    toJsonNode(json.toString())
  }

  def toJsonNode(json: String): IO[CredentialSchemaError, JsonNode] = {
    for {
      mapper <- ZIO.attempt(new ObjectMapper()).mapError(t => UnexpectedError(t.getMessage))
      jsonSchemaNode <- ZIO
        .attempt(mapper.readTree(json))
        .mapError(t => JsonSchemaParsingError(t.getMessage))
    } yield jsonSchemaNode
  }
}
