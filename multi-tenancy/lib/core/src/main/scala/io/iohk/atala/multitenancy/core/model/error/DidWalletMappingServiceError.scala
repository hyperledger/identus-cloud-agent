package io.iohk.atala.multitenancy.core.model.error

import java.util.UUID

sealed trait DidWalletMappingServiceError

object DidWalletMappingServiceError {
  final case class RepositoryError(cause: Throwable) extends DidWalletMappingServiceError
  final case class RecordIdNotFound(recordId: UUID) extends DidWalletMappingServiceError
  final case class UnexpectedError(msg: String) extends DidWalletMappingServiceError
}
