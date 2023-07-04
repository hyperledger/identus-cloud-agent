package io.iohk.atala.pollux.core.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.*
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError.*
import io.iohk.atala.pollux.core.service.URIDereferencer
import zio.*
import zio.json.*

import java.net.URI
import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

type Schema = zio.json.ast.Json

/** @param guid
  *   Globally unique identifier of the CredentialSchema object It's calculated as a UUID from string that contains the
  *   following fields: author, id and version
  * @param id
  *   Locally unique identifier of the CredentialSchema. It is UUID When the version of the credential schema changes
  *   this `id` keeps the same value
  * @param name
  *   Human readable name of the CredentialSchema
  * @param version
  *   Version of the CredentialSchema
  * @param author
  *   DID of the CredentialSchema's author
  * @param authored
  *   Datetime stamp of the schema creation
  * @param tags
  *   Tags of the CredentialSchema used for convenient lookup
  * @param description
  *   Human readable description of the schema
  * @param schema
  *   Internal schema object that depends on concrete implementation For W3C JsonSchema it is a JsonSchema object For
  *   AnonCreds schema is a AnonCreds schema
  */
case class CredentialSchema(
    guid: UUID,
    id: UUID,
    name: String,
    version: String,
    author: String,
    authored: OffsetDateTime,
    tags: Seq[String],
    description: String,
    `type`: String,
    schema: Schema
) {
  def longId = CredentialSchema.makeLongId(author, id, version)
}

object CredentialSchema {

  def makeLongId(author: String, id: UUID, version: String) =
    s"$author/${id.toString}?version=${version}"
  def makeGUID(author: String, id: UUID, version: String) =
    UUID.nameUUIDFromBytes(makeLongId(author, id, version).getBytes)
  def make(in: Input): ZIO[Any, Nothing, CredentialSchema] = {
    for {
      id <- zio.Random.nextUUID
      cs <- make(id, in)
    } yield cs
  }
  def make(id: UUID, in: Input): ZIO[Any, Nothing, CredentialSchema] = {
    for {
      ts <- zio.Clock.currentDateTime.map(
        _.atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime
      )
      guid = makeGUID(in.author, id, in.version)
    } yield CredentialSchema(
      guid = guid,
      id = id,
      name = in.name,
      version = in.version,
      author = in.author,
      authored = ts,
      tags = in.tags,
      description = in.description,
      `type` = in.`type`,
      schema = in.schema
    )
  }

  val defaultAgentDid = "did:prism:agent"

  case class Input(
      name: String,
      version: String,
      description: String,
      authored: Option[OffsetDateTime],
      tags: Seq[String],
      author: String = defaultAgentDid,
      `type`: String,
      schema: Schema
  )

  case class Filter(
      author: Option[String] = None,
      name: Option[String] = None,
      version: Option[String] = None,
      tags: Option[String] = None
  )

  case class FilteredEntries(entries: Seq[CredentialSchema], count: Long, totalCount: Long)

  given JsonEncoder[CredentialSchema] = DeriveJsonEncoder.gen[CredentialSchema]
  given JsonDecoder[CredentialSchema] = DeriveJsonDecoder.gen[CredentialSchema]

  val VC_JSON_SCHEMA_URI = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json"

  def validateClaims(
      schemaId: String,
      claims: String,
      uriDereferencer: URIDereferencer
  ): IO[CredentialSchemaError, Unit] = {
    for {
      uri <- ZIO.attempt(new URI(schemaId)).mapError(t => URISyntaxError(t.getMessage))
      content <- uriDereferencer.dereference(uri).mapError(err => UnexpectedError(err.toString))
      vcSchema <- parseAndValidateCredentialSchema(content)
      jsonSchema <- validateJsonSchema(vcSchema.schema)
      _ <- validateClaims(jsonSchema, claims)
    } yield ()
  }

  def parseAndValidateCredentialSchema(vcSchemaString: String): IO[CredentialSchemaError, CredentialSchema] = {
    for {
      vcSchema <- vcSchemaString.fromJson[CredentialSchema] match
        case Left(error)     => ZIO.fail(CredentialSchemaParsingError(s"VC Schema parsing error: $error"))
        case Right(vcSchema) => ZIO.succeed(vcSchema)
      _ <- validateCredentialSchema(vcSchema)
    } yield vcSchema
  }

  def validateCredentialSchema(vcSchema: CredentialSchema): IO[CredentialSchemaError, Unit] = for {
    _ <-
      if (vcSchema.`type` == VC_JSON_SCHEMA_URI) ZIO.unit
      else ZIO.fail(UnsupportedCredentialSchemaType(s"VC Schema type should be $VC_JSON_SCHEMA_URI"))
    _ <- validateJsonSchema(vcSchema.schema)
  } yield ()

  def validateJsonSchema(jsonSchema: Schema): IO[CredentialSchemaError, JsonSchema] = {
    for {
      mapper <- ZIO.attempt(new ObjectMapper()).mapError(t => UnexpectedError(t.getMessage))
      jsonSchemaNode <- ZIO
        .attempt(mapper.readTree(jsonSchema.toString()))
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
    } yield jsonSchema
  }

  def validateClaims(jsonSchema: JsonSchema, claims: String): IO[CredentialSchemaError, Unit] = {
    import scala.jdk.CollectionConverters.*
    for {
      mapper <- ZIO.attempt(new ObjectMapper()).mapError(t => UnexpectedError(t.getMessage))

      // Convert claims to JsonNode
      jsonClaims <- ZIO
        .attempt(mapper.readTree(claims))
        .mapError(t => ClaimsParsingError(t.getMessage))

      // Validate claims JsonNode
      validationMessages <- ZIO
        .attempt(jsonSchema.validate(jsonClaims).asScala.toSeq)
        .mapError(t => ClaimsValidationError(Seq(t.getMessage)))

      validationResult <-
        if (validationMessages.isEmpty) ZIO.unit
        else ZIO.fail(ClaimsValidationError(validationMessages.map(_.getMessage)))
    } yield validationResult
  }

}
