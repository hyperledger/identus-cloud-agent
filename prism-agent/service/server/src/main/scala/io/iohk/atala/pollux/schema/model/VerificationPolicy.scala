package io.iohk.atala.pollux.schema.model

import sttp.model.Uri
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedName}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

case class VerificationPolicy(
    self: String,
    kind: String,
    id: String,
    name: String,
    attributes: List[String],
    issuerDIDs: List[String],
    credentialTypes: List[String],
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime
) {
  def update(in: VerificationPolicyInput): VerificationPolicy = {
    copy(
      name = in.name,
      attributes = in.attributes,
      issuerDIDs = in.issuerDIDs,
      credentialTypes = in.credentialTypes,
      updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
    )
  }

  def withBaseUri(base: Uri) = withSelf(base.addPath(id).toString)

  def withUri(uri: Uri) = withSelf(uri.toString)

  def withSelf(self: String) = copy(self = self)
}

object VerificationPolicy {
  def apply(in: VerificationPolicyInput): VerificationPolicy =
    VerificationPolicy(
      self = "to be defined",
      kind = "VerificationPolicy",
      id = in.id.getOrElse(UUID.randomUUID().toString),
      name = in.name,
      attributes = in.attributes,
      issuerDIDs = in.issuerDIDs,
      credentialTypes = in.credentialTypes,
      createdAt = in.createdAt.getOrElse(OffsetDateTime.now(ZoneOffset.UTC)),
      updatedAt = in.updatedAt.getOrElse(OffsetDateTime.now(ZoneOffset.UTC))
    )

  given encoder: zio.json.JsonEncoder[VerificationPolicy] = DeriveJsonEncoder.gen[VerificationPolicy]
  given decoder: zio.json.JsonDecoder[VerificationPolicy] = DeriveJsonDecoder.gen[VerificationPolicy]
  given schema: Schema[VerificationPolicy] = Schema.derived

  case class Filter(
      name: Option[String],
      attributes: Option[String],
      issuerDIDs: Option[String],
      credentialTypes: Option[String]
  ) {
    def predicate(vp: VerificationPolicy): Boolean = {
      name.forall(vp.name == _) &&
      attributes.map(_.split(',')).forall(vp.attributes.intersect(_).nonEmpty) &&
      issuerDIDs.map(_.split(',')).forall(vp.issuerDIDs.intersect(_).nonEmpty) &&
      credentialTypes.map(_.split(',')).forall(vp.credentialTypes.intersect(_).nonEmpty)
    }
  }
}

case class VerificationPolicyPage(
    self: String,
    kind: String,
    pageOf: String,
    next: Option[String],
    previous: Option[String],
    contents: List[VerificationPolicy]
)

object VerificationPolicyPage {
  given encoder: zio.json.JsonEncoder[VerificationPolicyPage] =
    DeriveJsonEncoder.gen[VerificationPolicyPage]

  given decoder: zio.json.JsonDecoder[VerificationPolicyPage] =
    DeriveJsonDecoder.gen[VerificationPolicyPage]

  given schema: Schema[VerificationPolicyPage] =
    Schema.derived
}

case class VerificationPolicyInput(
    id: Option[String],
    name: String,
    attributes: List[String],
    issuerDIDs: List[String],
    credentialTypes: List[String],
    createdAt: Option[OffsetDateTime] = None,
    updatedAt: Option[OffsetDateTime] = None
)

object VerificationPolicyInput {
  given encoder: zio.json.JsonEncoder[VerificationPolicyInput] =
    DeriveJsonEncoder.gen[VerificationPolicyInput]

  given decoder: zio.json.JsonDecoder[VerificationPolicyInput] =
    DeriveJsonDecoder.gen[VerificationPolicyInput]

  given schema: Schema[VerificationPolicyInput] = Schema.derived
}
