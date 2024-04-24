package org.hyperledger.identus.verification.controller.http

import org.hyperledger.identus.pollux.core.service.verification.VcVerificationResult as ServiceVcVerificationResult
import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class VcVerificationResult(
    verification: VcVerification,
    success: Boolean
)
object VcVerificationResult {
  given encoder: JsonEncoder[VcVerificationResult] =
    DeriveJsonEncoder.gen[VcVerificationResult]

  given decoder: JsonDecoder[VcVerificationResult] =
    DeriveJsonDecoder.gen[VcVerificationResult]

  given schema: Schema[VcVerificationResult] = Schema.derived

  def toService(result: ServiceVcVerificationResult): VcVerificationResult = {
    VcVerificationResult(
      verification = VcVerification.toService(result.verification),
      success = result.success
    )
  }
}
