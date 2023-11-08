package io.iohk.atala.iam.wallet.http.model

import sttp.tapir.*
import sttp.tapir.Schema.annotations.{description, encodedExample, validate}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.util.UUID

// TODO: annotate
final case class CreateWalletUmaPermissionRequest(
    subject: UUID
)

object CreateWalletUmaPermissionRequest {
  given encoder: JsonEncoder[CreateWalletUmaPermissionRequest] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[CreateWalletUmaPermissionRequest] = DeriveJsonDecoder.gen
  given schema: Schema[CreateWalletUmaPermissionRequest] = Schema.derived
}
