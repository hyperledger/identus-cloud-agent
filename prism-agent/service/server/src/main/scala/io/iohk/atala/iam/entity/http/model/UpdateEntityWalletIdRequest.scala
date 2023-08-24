package io.iohk.atala.iam.entity.http.model

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.iam.entity.http.model.UpdateEntityWalletIdRequest.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample, validate, validateEach}
import sttp.tapir.Validator.*
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.util.UUID

case class UpdateEntityWalletIdRequest(
    @description(annotations.walletId.description)
    @encodedExample(annotations.walletId.example)
    walletId: UUID
)

object UpdateEntityWalletIdRequest {
  given encoder: JsonEncoder[UpdateEntityWalletIdRequest] =
    DeriveJsonEncoder.gen[UpdateEntityWalletIdRequest]

  given decoder: JsonDecoder[UpdateEntityWalletIdRequest] =
    DeriveJsonDecoder.gen[UpdateEntityWalletIdRequest]

  given schema: Schema[UpdateEntityWalletIdRequest] = Schema.derived

  object annotations {
    object walletId
        extends Annotation[UUID](
          description = "The walletId owned by the entity",
          example = UUID.fromString("00000000-0000-0000-0000-000000000000")
        )
  }
}
