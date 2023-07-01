package io.iohk.atala.pollux.core.model.schema

import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError.*
import io.iohk.atala.pollux.core.model.schema.`type`.{
  AnoncredSchemaType,
  CredentialJsonSchemaType,
  CredentialSchemaType
}
import io.iohk.atala.pollux.core.model.schema.validator.CredentialJsonSchemaValidator
import io.iohk.atala.pollux.core.service.URIDereferencer
import zio.*
import zio.json.*
import zio.prelude.Validation

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

  def validateClaims(
      schemaId: String,
      claims: String,
      uriDereferencer: URIDereferencer
  ): IO[CredentialSchemaError, Unit] = {
    for {
      uri <- ZIO.attempt(new URI(schemaId)).mapError(t => URISyntaxError(t.getMessage))
      content <- uriDereferencer.dereference(uri).mapError(err => UnexpectedError(err.toString))
      vcSchema <- parseCredentialSchema(content)
      resolvedSchemaType <- resolveCredentialSchemaType(vcSchema.`type`)
      _ <- Validation
        .fromPredicateWith(
          CredentialSchemaParsingError(
            s"Only ${CredentialJsonSchemaType.`type`} schema type can be used to verify claims"
          )
        )(resolvedSchemaType.`type`)(`type` => `type` == CredentialJsonSchemaType.`type`)
        .toZIO
      schemaValidator <- CredentialJsonSchemaValidator.from(vcSchema.schema)
      _ <- schemaValidator.validate(claims)
    } yield ()
  }

  private val supportedCredentialSchemaTypes: Map[String, CredentialSchemaType] =
    IndexedSeq(CredentialJsonSchemaType, AnoncredSchemaType)
      .map(credentialSchemaType => (credentialSchemaType.`type`, credentialSchemaType))
      .toMap

  def resolveCredentialSchemaType(`type`: String): IO[CredentialSchemaError, CredentialSchemaType] = {
    ZIO
      .fromOption(supportedCredentialSchemaTypes.get(`type`))
      .mapError(_ => UnsupportedCredentialSchemaType(s"Unsupported VC Schema type ${`type`}"))
  }

  def validateCredentialSchema(vcSchema: CredentialSchema): IO[CredentialSchemaError, Unit] = {
    for {
      resolvedSchemaType <- resolveCredentialSchemaType(vcSchema.`type`)
      _ <- resolvedSchemaType.validate(vcSchema.schema)
    } yield ()
  }

  def parseCredentialSchema(vcSchemaString: String): IO[CredentialSchemaError, CredentialSchema] =
    ZIO
      .fromEither(vcSchemaString.fromJson[CredentialSchema])
      .mapError(error => CredentialSchemaParsingError(s"VC Schema parsing error: $error"))
}
