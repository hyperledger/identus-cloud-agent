package io.iohk.atala.iam.wallet.http.model

import io.iohk.atala.api.http.Annotation
import sttp.tapir.*
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.util.UUID

final case class WalletDetail(
    @description(WalletDetail.annotations.id.description)
    @encodedExample(WalletDetail.annotations.id.example)
    id: UUID
)

object WalletDetail {
  given encoder: JsonEncoder[WalletDetail] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[WalletDetail] = DeriveJsonDecoder.gen
  given schema: Schema[WalletDetail] = Schema.derived

  object annotations {
    object id
        extends Annotation[UUID](
          description = "A wallet ID",
          example = UUID.fromString("00000000-0000-0000-0000-000000000000")
        )
  }
}

final case class WalletDetailPage(
    self: String,
    kind: String = "WalletDetailPage",
    pageOf: String,
    next: Option[String] = None,
    previous: Option[String] = None,
    contents: Seq[WalletDetail]
)

object WalletDetailPage {
  given encoder: JsonEncoder[WalletDetailPage] = DeriveJsonEncoder.gen[WalletDetailPage]
  given decoder: JsonDecoder[WalletDetailPage] = DeriveJsonDecoder.gen[WalletDetailPage]
  given schema: Schema[WalletDetailPage] = Schema.derived
}
