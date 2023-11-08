package io.iohk.atala.iam.wallet.http.model

import sttp.tapir.*
import sttp.tapir.Schema.annotations.{description, encodedExample, validate}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

// TODO: annotate
final case class UmaPermission()

object UmaPermission {
  given encoder: JsonEncoder[UmaPermission] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[UmaPermission] = DeriveJsonDecoder.gen
  given schema: Schema[UmaPermission] = Schema.derived
}
