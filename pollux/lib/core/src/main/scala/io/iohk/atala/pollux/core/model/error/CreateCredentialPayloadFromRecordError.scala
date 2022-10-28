package io.iohk.atala.pollux.core.model.error

sealed trait CreateCredentialPayloadFromRecordError 

object CreateCredentialPayloadFromRecordError {
  final case class RepositoryError(cause: Throwable) extends CreateCredentialPayloadFromRecordError
}
