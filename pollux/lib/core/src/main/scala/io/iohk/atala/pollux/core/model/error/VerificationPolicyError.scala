package io.iohk.atala.pollux.core.model.error

import java.util.UUID

sealed trait VerificationPolicyError
object VerificationPolicyError {
  final case class RepositoryError(cause: Throwable) extends VerificationPolicyError
  final case class NotFoundError(id: UUID) extends VerificationPolicyError
  final case class UnexpectedError(cause: Throwable) extends VerificationPolicyError
}
