package org.hyperledger.identus.verification.controller.http

import sttp.tapir.Schema
import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder

final case class ParameterizableVcVerification(
    verification: VcVerification,
    parameter: Option[VcVerificationParameter]
)
object ParameterizableVcVerification {
  given encoder: JsonEncoder[ParameterizableVcVerification] =
    DeriveJsonEncoder.gen[ParameterizableVcVerification]

  given decoder: JsonDecoder[ParameterizableVcVerification] =
    DeriveJsonDecoder.gen[ParameterizableVcVerification]

  given schema: Schema[ParameterizableVcVerification] = Schema.derived
}
