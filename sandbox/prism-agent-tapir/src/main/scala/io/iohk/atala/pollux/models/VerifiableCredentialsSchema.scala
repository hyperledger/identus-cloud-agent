package io.iohk.atala.pollux.models

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

import java.time.ZonedDateTime
import java.util.UUID

case class VerifiableCredentialsSchemaInput(
    id: Option[UUID],
    name: String,
    version: String,
    description: Option[String],
    attributes: List[String],
    authored: Option[ZonedDateTime],
    tags: List[String]
)
object VerifiableCredentialsSchemaInput {
  given encoder: zio.json.JsonEncoder[VerifiableCredentialsSchemaInput] =
    DeriveJsonEncoder.gen[VerifiableCredentialsSchemaInput]
  given decoder: zio.json.JsonDecoder[VerifiableCredentialsSchemaInput] =
    DeriveJsonDecoder.gen[VerifiableCredentialsSchemaInput]
}

case class VerifiableCredentialsSchema(
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

object VerifiableCredentialsSchema {
  def apply(in: VerifiableCredentialsSchemaInput): VerifiableCredentialsSchema =
    VerifiableCredentialsSchema(
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

  given encoder: zio.json.JsonEncoder[VerifiableCredentialsSchema] = DeriveJsonEncoder.gen[VerifiableCredentialsSchema]
  given decoder: zio.json.JsonDecoder[VerifiableCredentialsSchema] = DeriveJsonDecoder.gen[VerifiableCredentialsSchema]
}

case class Proof(
    `type`: String,
    created: ZonedDateTime,
    verificationMethod: String,
    proofPurpose: String,
    proofValue: String,
    domain: Option[String]
)

object Proof {
  given encoder: zio.json.JsonEncoder[Proof] = DeriveJsonEncoder.gen[Proof]
  given decoder: zio.json.JsonDecoder[Proof] = DeriveJsonDecoder.gen[Proof]
}
