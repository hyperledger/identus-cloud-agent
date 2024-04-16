package io.iohk.atala.pollux.core.service.verification

import zio.*

trait VcVerificationService {
  def verify(request: List[VcVerificationRequest]): IO[VcVerificationServiceError, List[VcVerificationResult]]
}

sealed trait VcVerificationParameter

case class AudienceParameter(aud: String) extends VcVerificationParameter

final case class VcVerificationRequest(
    credential: String,
    verification: VcVerification,
    parameter: Option[VcVerificationParameter]
)

final case class VcVerificationResult(
    credential: String,
    verification: VcVerification,
    success: Boolean
)
