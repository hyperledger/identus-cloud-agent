package io.iohk.atala.pollux.schema.model

import io.iohk.atala.pollux.core.model
import io.iohk.atala.pollux.core.model.CredentialSchemaAndTrustedIssuersConstraint
import sttp.model.Uri
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedName}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

//TODO: All these classes should be moved to the Pollux library into api package
case class VerificationPolicyConstraint(schemaId: String, trustedIssuers: Seq[String])

object VerificationPolicyConstraint {
  given encoder: zio.json.JsonEncoder[VerificationPolicyConstraint] =
    DeriveJsonEncoder.gen[VerificationPolicyConstraint]

  given decoder: zio.json.JsonDecoder[VerificationPolicyConstraint] =
    DeriveJsonDecoder.gen[VerificationPolicyConstraint]

  given schema: Schema[VerificationPolicyConstraint] = Schema.derived

  extension (constraint: model.VerificationPolicyConstraint) {
    def toSchema(): VerificationPolicyConstraint = {
      VerificationPolicyConstraint(constraint)
    }
  }

  given Conversion[model.VerificationPolicyConstraint, VerificationPolicyConstraint] with {
    override def apply(constraint: model.VerificationPolicyConstraint): VerificationPolicyConstraint =
      VerificationPolicyConstraint(constraint)
  }

  def apply(constraint: model.VerificationPolicyConstraint): VerificationPolicyConstraint = {
    constraint match {
      case CredentialSchemaAndTrustedIssuersConstraint(schemaId, trustedIssuers) =>
        VerificationPolicyConstraint(schemaId, trustedIssuers)
    }
  }
}

case class VerificationPolicy(
    self: String,
    kind: String,
    id: UUID,
    nonce: Int = 0,
    name: String,
    description: String,
    createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    updatedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    constraints: Seq[VerificationPolicyConstraint]
) {
  def update(in: VerificationPolicyInput): VerificationPolicy = {
    copy(
      id = in.id.getOrElse(UUID.randomUUID()),
      name = in.name,
      description = in.description,
      constraints = in.constraints
    )
  }

  def withBaseUri(base: Uri) = withSelf(base.addPath(id.toString).toString)

  def withUri(uri: Uri) = withSelf(uri.toString)

  def withSelf(self: String) = copy(self = self)
}

object VerificationPolicy {
  def apply(in: VerificationPolicyInput): VerificationPolicy =
    VerificationPolicy(
      self = "",
      kind = "VerificationPolicy",
      id = in.id.getOrElse(UUID.randomUUID()),
      name = in.name,
      description = in.description,
      constraints = in.constraints
    )

  given encoder: zio.json.JsonEncoder[VerificationPolicy] = DeriveJsonEncoder.gen[VerificationPolicy]
  given decoder: zio.json.JsonDecoder[VerificationPolicy] = DeriveJsonDecoder.gen[VerificationPolicy]
  given schema: Schema[VerificationPolicy] = Schema.derived

  import VerificationPolicyConstraint._
  extension (vp: model.VerificationPolicy) {
    def toSchema(): VerificationPolicy = {
      VerificationPolicy(
        self = "",
        kind = "VerificationPolicy",
        id = vp.id,
        nonce = vp.nonce,
        name = vp.name,
        description = vp.description,
        createdAt = vp.createdAt,
        updatedAt = vp.updatedAt,
        constraints = vp.constrains.map(VerificationPolicyConstraint(_)).toList
      )
    }
  }

  // TODO: This filter is used for InMemory implementation only, think about removing it
  case class Filter(
      name: Option[String]
  ) {

    // TODO: This filter is used for InMemory implementation only, think about removing it
    def predicate(vp: VerificationPolicy): Boolean = {
      name.forall(vp.name == _)
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
    id: Option[UUID],
    name: String,
    description: String,
    constraints: List[VerificationPolicyConstraint],
)

object VerificationPolicyInput {
  given encoder: zio.json.JsonEncoder[VerificationPolicyInput] =
    DeriveJsonEncoder.gen[VerificationPolicyInput]

  given decoder: zio.json.JsonDecoder[VerificationPolicyInput] =
    DeriveJsonDecoder.gen[VerificationPolicyInput]

  given schema: Schema[VerificationPolicyInput] = Schema.derived
}
