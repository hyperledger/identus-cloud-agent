package io.iohk.atala.castor.controller.http

import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}
import io.iohk.atala.shared.models.HexStrings.HexString
import io.iohk.atala.castor.core.model.did.ScheduleDIDOperationOutcome

final case class DIDOperationResponse(
    scheduledOperation: DidOperationSubmission
)

object DIDOperationResponse {
  given encoder: JsonEncoder[DIDOperationResponse] = DeriveJsonEncoder.gen[DIDOperationResponse]
  given decoder: JsonDecoder[DIDOperationResponse] = DeriveJsonDecoder.gen[DIDOperationResponse]
  given schema: Schema[DIDOperationResponse] = Schema.derived

  given Conversion[ScheduleDIDOperationOutcome, DIDOperationResponse] = { outcome =>
    DIDOperationResponse(scheduledOperation =
      DidOperationSubmission(
        id = HexString.fromByteArray(outcome.operationId.toArray).toString,
        didRef = outcome.did.toString
      )
    )
  }
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
