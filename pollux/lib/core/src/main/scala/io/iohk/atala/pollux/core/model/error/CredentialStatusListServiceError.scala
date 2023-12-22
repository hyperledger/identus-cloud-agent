package io.iohk.atala.pollux.core.model.error

import io.iohk.atala.pollux.core.model.DidCommID
import io.iohk.atala.pollux.vc.jwt.W3cCredentialPayload

import java.util.UUID

sealed trait CredentialStatusListServiceError

object CredentialStatusListServiceError {
  final case class RepositoryError(cause: Throwable) extends CredentialStatusListServiceError
  final case class RecordIdNotFound(id: UUID) extends CredentialStatusListServiceError
}
