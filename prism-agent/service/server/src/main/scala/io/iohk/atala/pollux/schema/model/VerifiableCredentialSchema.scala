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
  def apply(in: VerifiableCredentialSchema.Input): VerifiableCredentialSchema =
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

  case class Input(
      id: Option[UUID],
      name: String,
      version: String,
      description: Option[String],
      attributes: List[String],
      authored: Option[ZonedDateTime],
      tags: List[String]
  )

  object Input {
    given encoder: zio.json.JsonEncoder[Input] =
      DeriveJsonEncoder.gen[Input]

    given decoder: zio.json.JsonDecoder[Input] =
      DeriveJsonDecoder.gen[Input]

    given schema: Schema[Input] = Schema.derived
  }

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

  case class Page(
      self: String,
      kind: String,
      pageOf: String,
      next: Option[String],
      previous: Option[String],
      contents: List[VerifiableCredentialSchema]
  )

  object Page {
    given encoder: zio.json.JsonEncoder[Page] = DeriveJsonEncoder.gen[Page]
    given decoder: zio.json.JsonDecoder[Page] = DeriveJsonDecoder.gen[Page]
    given schema: sttp.tapir.Schema[Page] = Schema.derived
  }
}
