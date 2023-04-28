package io.iohk.atala.castor.controller.http

import io.iohk.atala.agent.walletapi.model as walletDomain
import io.iohk.atala.agent.walletapi.model.ManagedDIDDetail
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.castor.core.model.did as castorDomain
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.shared.utils.Traverse.*
import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}

final case class ManagedDID(
    did: String,
    longFormDid: Option[String] = None,
    status: String // TODO: use enum
)

object ManagedDID {
  given encoder: JsonEncoder[ManagedDID] = DeriveJsonEncoder.gen[ManagedDID]
  given decoder: JsonDecoder[ManagedDID] = DeriveJsonDecoder.gen[ManagedDID]
  given schema: Schema[ManagedDID] = Schema.derived

  given Conversion[ManagedDIDDetail, ManagedDID] = { didDetail =>
    val (longFormDID, status) = didDetail.state match {
      case ManagedDIDState.Created(operation) => Some(PrismDID.buildLongFormFromOperation(operation)) -> "CREATED"
      case ManagedDIDState.PublicationPending(operation, _) =>
        Some(PrismDID.buildLongFormFromOperation(operation)) -> "PUBLICATION_PENDING"
      case ManagedDIDState.Published(_, _) => None -> "PUBLISHED"
    }
    ManagedDID(
      did = didDetail.did.toString,
      longFormDid = longFormDID.map(_.toString),
      status = status
    )
  }
}

final case class ManagedDIDPage(
    self: String,
    kind: String = "ManagedDIDPage",
    pageOf: String,
    next: Option[String] = None,
    previous: Option[String] = None,
    contents: Seq[ManagedDID]
)

object ManagedDIDPage {
  given encoder: JsonEncoder[ManagedDIDPage] = DeriveJsonEncoder.gen[ManagedDIDPage]
  given decoder: JsonDecoder[ManagedDIDPage] = DeriveJsonDecoder.gen[ManagedDIDPage]
  given schema: Schema[ManagedDIDPage] = Schema.derived
}

final case class CreateManagedDidRequest(
    documentTemplate: CreateManagedDidRequestDocumentTemplate
)

object CreateManagedDidRequest {
  given encoder: JsonEncoder[CreateManagedDidRequest] = DeriveJsonEncoder.gen[CreateManagedDidRequest]
  given decoder: JsonDecoder[CreateManagedDidRequest] = DeriveJsonDecoder.gen[CreateManagedDidRequest]
  given schema: Schema[CreateManagedDidRequest] = Schema.derived
}

final case class CreateManagedDidRequestDocumentTemplate(
    publicKeys: Seq[ManagedDIDKeyTemplate],
    services: Seq[Service]
)

object CreateManagedDidRequestDocumentTemplate {
  given encoder: JsonEncoder[CreateManagedDidRequestDocumentTemplate] =
    DeriveJsonEncoder.gen[CreateManagedDidRequestDocumentTemplate]
  given decoder: JsonDecoder[CreateManagedDidRequestDocumentTemplate] =
    DeriveJsonDecoder.gen[CreateManagedDidRequestDocumentTemplate]
  given schema: Schema[CreateManagedDidRequestDocumentTemplate] = Schema.derived

  extension (template: CreateManagedDidRequestDocumentTemplate) {
    def toDomain: Either[String, walletDomain.ManagedDIDTemplate] = {
      for {
        services <- template.services.traverse(_.toDomain)
        publicKeys <- template.publicKeys.traverse(_.toDomain)
      } yield walletDomain.ManagedDIDTemplate(
        publicKeys = publicKeys,
        services = services
      )
    }
  }
}

final case class ManagedDIDKeyTemplate(
    id: String,
    purpose: String
)

object ManagedDIDKeyTemplate {
  given encoder: JsonEncoder[ManagedDIDKeyTemplate] = DeriveJsonEncoder.gen[ManagedDIDKeyTemplate]
  given decoder: JsonDecoder[ManagedDIDKeyTemplate] = DeriveJsonDecoder.gen[ManagedDIDKeyTemplate]
  given schema: Schema[ManagedDIDKeyTemplate] = Schema.derived

  extension (publicKeyTemplate: ManagedDIDKeyTemplate) {
    def toDomain: Either[String, walletDomain.DIDPublicKeyTemplate] = {
      for {
        purpose <- castorDomain.VerificationRelationship
          .parseString(publicKeyTemplate.purpose)
          .toRight(s"unsupported verificationRelationship ${publicKeyTemplate.purpose}")
      } yield walletDomain.DIDPublicKeyTemplate(
        id = publicKeyTemplate.id,
        purpose = purpose
      )
    }
  }
}

final case class CreateManagedDIDResponse(
    longFormDid: String
)

object CreateManagedDIDResponse {
  given encoder: JsonEncoder[CreateManagedDIDResponse] = DeriveJsonEncoder.gen[CreateManagedDIDResponse]
  given decoder: JsonDecoder[CreateManagedDIDResponse] = DeriveJsonDecoder.gen[CreateManagedDIDResponse]
  given schema: Schema[CreateManagedDIDResponse] = Schema.derived
}
