package io.iohk.atala.pollux.schema.model

import sttp.model.Uri._
import sttp.model.Uri
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedName}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

import java.time.ZonedDateTime
import java.util.UUID

case class VerifiableCredentialSchema(
    id: UUID,
    name: String,
    version: String,
    tags: List[String],
    description: Option[String],
    attributes: List[String],
    author: String,
    authored: ZonedDateTime,
    proof: Option[Proof],
    kind: String = "VerifiableCredentialSchema",
    self: String = ""
) {
  def withBaseUri(base: Uri) = withSelf(base.addPath(id.toString).toString)
  def withSelf(self: String) = copy(self = self)
}

object VerifiableCredentialSchema {
  def apply(in: VerifiableCredentialSchemaInput): VerifiableCredentialSchema =
    VerifiableCredentialSchema(
      id = in.id.getOrElse(UUID.randomUUID()),
      name = in.name,
      version = in.version,
      tags = in.tags,
      description = in.description,
      attributes = in.attributes,
      author = "Prism Agent",
      authored = in.authored.getOrElse(ZonedDateTime.now()),
      proof = None
    )

  given encoder: zio.json.JsonEncoder[VerifiableCredentialSchema] =
    DeriveJsonEncoder.gen[VerifiableCredentialSchema]
  given decoder: zio.json.JsonDecoder[VerifiableCredentialSchema] =
    DeriveJsonDecoder.gen[VerifiableCredentialSchema]
  given schema: Schema[VerifiableCredentialSchema] = Schema.derived

  case class Filter(
      author: Option[String],
      name: Option[String],
      tags: Option[String]
  ) {
    def predicate(vcs: VerifiableCredentialSchema): Boolean =
      name.forall(_ == vcs.name) &&
        author.forall(_ == vcs.author) &&
        tags.map(_.split(',')).forall(vcs.tags.intersect(_).nonEmpty)
  }
}

case class VerifiableCredentialSchemaInput(
    id: Option[UUID],
    name: String,
    version: String,
    description: Option[String],
    attributes: List[String],
    authored: Option[ZonedDateTime],
    tags: List[String]
)

object VerifiableCredentialSchemaInput {
  given encoder: zio.json.JsonEncoder[VerifiableCredentialSchemaInput] =
    DeriveJsonEncoder.gen[VerifiableCredentialSchemaInput]
  given decoder: zio.json.JsonDecoder[VerifiableCredentialSchemaInput] =
    DeriveJsonDecoder.gen[VerifiableCredentialSchemaInput]
  given schema: Schema[VerifiableCredentialSchemaInput] = Schema.derived
}

case class VerifiableCredentialSchemaPage(
    contents: List[VerifiableCredentialSchema],
    kind: String = "VerifiableCredentialSchemaPage",
    self: String = "",
    pageOf: String = "",
    next: Option[String] = None,
    previous: Option[String] = None
) {
  def withSelf(self: String) = copy(self = self)
}

object VerifiableCredentialSchemaPage {
  given encoder: zio.json.JsonEncoder[VerifiableCredentialSchemaPage] =
    DeriveJsonEncoder.gen[VerifiableCredentialSchemaPage]
  given decoder: zio.json.JsonDecoder[VerifiableCredentialSchemaPage] =
    DeriveJsonDecoder.gen[VerifiableCredentialSchemaPage]
  given schema: sttp.tapir.Schema[VerifiableCredentialSchemaPage] = Schema.derived
}
