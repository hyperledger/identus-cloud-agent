package io.iohk.atala.pollux.credentialschema.http

import io.iohk.atala.pollux.core.model
import io.iohk.atala.pollux.core.model.CredentialSchema.Input
import sttp.model.Uri
import sttp.model.Uri.*
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedName}
import sttp.tapir.json.zio.schemaForZioJsonValue
import zio.json.ast.Json
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

case class CredentialSchemaResponse(
    guid: UUID,
    id: String,
    longId: Option[String],
    name: String,
    version: String,
    tags: Seq[String],
    description: String,
    `type`: String,
    schema: Json,
    author: String,
    authored: OffsetDateTime,
    proof: Option[Proof],
    kind: String = "CredentialSchema",
    self: String = ""
) {
  def withBaseUri(base: Uri) = withSelf(base.addPath(guid.toString).toString)
  def withSelf(self: String) = copy(self = self)
}

object CredentialSchemaResponse {

  def fromDomain(cs: model.CredentialSchema): CredentialSchemaResponse =
    CredentialSchemaResponse(
      guid = cs.guid,
      id = cs.id.toString,
      longId = Option(cs.longId),
      name = cs.name,
      version = cs.version,
      tags = cs.tags,
      description = cs.description,
      `type` = cs.`type`,
      schema = cs.schema,
      author = cs.author,
      authored = cs.authored,
      proof = None
    )

  given scala.Conversion[model.CredentialSchema, CredentialSchemaResponse] = fromDomain

  given encoder: zio.json.JsonEncoder[CredentialSchemaResponse] =
    DeriveJsonEncoder.gen[CredentialSchemaResponse]
  given decoder: zio.json.JsonDecoder[CredentialSchemaResponse] =
    DeriveJsonDecoder.gen[CredentialSchemaResponse]
  given schema: Schema[CredentialSchemaResponse] = Schema.derived
}

case class FilterInput(
    author: Option[String] = None,
    name: Option[String] = None,
    version: Option[String] = None,
    tags: Option[String] = None
) {
  def toDomain = model.CredentialSchema.Filter(author, name, version, tags)
}

case class CredentialSchemaInput(
    name: String,
    version: String,
    description: Option[String],
    `type`: String,
    schema: zio.json.ast.Json,
    tags: Seq[String]
)

object CredentialSchemaInput {
  def toDomain(in: CredentialSchemaInput): Input =
    Input(
      name = in.name,
      version = in.version,
      tags = in.tags,
      description = in.description.getOrElse(""),
      `type` = in.`type`,
      schema = in.schema,
      author = "did:prism:agent",
      authored = None
    )
  given encoder: zio.json.JsonEncoder[CredentialSchemaInput] =
    DeriveJsonEncoder.gen[CredentialSchemaInput]
  given decoder: zio.json.JsonDecoder[CredentialSchemaInput] =
    DeriveJsonDecoder.gen[CredentialSchemaInput]
  given schema: Schema[CredentialSchemaInput] = Schema.derived
}

case class CredentialSchemaPageResponse(
    contents: List[CredentialSchemaResponse],
    kind: String = "CredentialSchemaPage",
    self: String = "",
    pageOf: String = "",
    next: Option[String] = None,
    previous: Option[String] = None
) {
  def withSelf(self: String) = copy(self = self)
}

object CredentialSchemaPageResponse {
  given encoder: zio.json.JsonEncoder[CredentialSchemaPageResponse] =
    DeriveJsonEncoder.gen[CredentialSchemaPageResponse]
  given decoder: zio.json.JsonDecoder[CredentialSchemaPageResponse] =
    DeriveJsonDecoder.gen[CredentialSchemaPageResponse]
  given schema: sttp.tapir.Schema[CredentialSchemaPageResponse] = Schema.derived
}
