package io.iohk.atala.castor.controller.http

import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}

final case class DIDOperationResponse(
    scheduledOperation: DidOperationSubmission
)

object DIDOperationResponse {
  given encoder: JsonEncoder[DIDOperationResponse] = DeriveJsonEncoder.gen[DIDOperationResponse]
  given decoder: JsonDecoder[DIDOperationResponse] = DeriveJsonDecoder.gen[DIDOperationResponse]
  given schema: Schema[DIDOperationResponse] = Schema.derived
}

final case class DidOperationSubmission(
    id: String,
    didRef: String
)

object DidOperationSubmission {
  given encoder: JsonEncoder[DidOperationSubmission] = DeriveJsonEncoder.gen[DidOperationSubmission]
  given decoder: JsonDecoder[DidOperationSubmission] = DeriveJsonDecoder.gen[DidOperationSubmission]
  given schema: Schema[DidOperationSubmission] = Schema.derived
}
