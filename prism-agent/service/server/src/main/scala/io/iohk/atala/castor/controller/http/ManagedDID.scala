package io.iohk.atala.castor.controller.http

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}
import sttp.tapir.Schema
import io.iohk.atala.agent.walletapi.model.ManagedDIDDetail
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.castor.core.model.did.PrismDID

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
