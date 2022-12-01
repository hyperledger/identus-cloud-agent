package io.iohk.atala.pollux.schema.model

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
    proof: Option[Proof]
)

object VerifiableCredentialSchema {
  def apply(in: VerificationCredentialSchemaInput): VerifiableCredentialSchema =
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

case class VerificationCredentialSchemaInput(
    id: Option[UUID],
    name: String,
    version: String,
    description: Option[String],
    attributes: List[String],
    authored: Option[ZonedDateTime],
    tags: List[String]
)

object VerificationCredentialSchemaInput {
  given encoder: zio.json.JsonEncoder[VerificationCredentialSchemaInput] =
    DeriveJsonEncoder.gen[VerificationCredentialSchemaInput]
  given decoder: zio.json.JsonDecoder[VerificationCredentialSchemaInput] =
    DeriveJsonDecoder.gen[VerificationCredentialSchemaInput]
  given schema: Schema[VerificationCredentialSchemaInput] = Schema.derived
}

case class VerifiableCredentialSchemaPage(
    self: String,
    kind: String,
    pageOf: String,
    next: Option[String],
    previous: Option[String],
    contents: List[VerifiableCredentialSchema]
)

object VerifiableCredentialSchemaPage {
  given encoder: zio.json.JsonEncoder[VerifiableCredentialSchemaPage] =
    DeriveJsonEncoder.gen[VerifiableCredentialSchemaPage]
  given decoder: zio.json.JsonDecoder[VerifiableCredentialSchemaPage] =
    DeriveJsonDecoder.gen[VerifiableCredentialSchemaPage]
  given schema: sttp.tapir.Schema[VerifiableCredentialSchemaPage] = Schema.derived
}
