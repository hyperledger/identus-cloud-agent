package io.iohk.atala.pollux.core.service.verification

sealed trait VcVerificationServiceError {
  def error: String
}

object VcVerificationServiceError {
  final case class UnexpectedError(error: String) extends VcVerificationServiceError
}
