package org.hyperledger.identus.pollux.core.model.error

import org.hyperledger.identus.pollux.core.model.DidCommID

import java.util.UUID

sealed trait CredentialStatusListServiceError {
  def toThrowable: Throwable = this match
    case CredentialStatusListServiceError.RepositoryError(cause) => cause
    case CredentialStatusListServiceError.RecordIdNotFound(id) =>
      new Exception(s"Credential status list with id: $id not found")
    case CredentialStatusListServiceError.IssueCredentialRecordNotFound(id) =>
      new Exception(s"Issue credential record with id: $id not found")
    case CredentialStatusListServiceError.JsonCredentialParsingError(cause) => cause

}

object CredentialStatusListServiceError {
  final case class RepositoryError(cause: Throwable) extends CredentialStatusListServiceError
  final case class RecordIdNotFound(id: UUID) extends CredentialStatusListServiceError
  final case class IssueCredentialRecordNotFound(id: DidCommID) extends CredentialStatusListServiceError
  final case class JsonCredentialParsingError(cause: Throwable) extends CredentialStatusListServiceError
}
