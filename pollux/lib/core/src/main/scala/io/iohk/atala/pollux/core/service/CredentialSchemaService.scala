package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.CredentialSchema
import io.iohk.atala.pollux.core.model.CredentialSchema.*
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError
import zio.IO

import java.util.UUID
trait CredentialSchemaService {
  type Result[T] = IO[CredentialSchemaService.Error, T]

  /** @param in
    *   CredentialSchema form for creating the instance
    * @return
    *   Created instance of the Credential Schema
    */
  def create(in: Input): Result[CredentialSchema]

  /** @param guid
    *   Globally unique UUID of the credential schema
    * @return
    *   The instance of the credential schema or credential service error
    */
  def getByGUID(guid: UUID): Result[CredentialSchema]

  def update(id: UUID, in: Input): Result[CredentialSchema]

  def delete(id: UUID): Result[CredentialSchema]

  def lookup(filter: Filter, skip: Int, limit: Int): Result[FilteredEntries]
}

object CredentialSchemaService {
  sealed trait Error

  object Error {
    def apply(throwable: Throwable): Error = RepositoryError(throwable)

    final case class RepositoryError(cause: Throwable) extends Error

    final case class NotFoundError(guid: Option[UUID] = None, id: Option[UUID] = None, message: String) extends Error

    object NotFoundError {
      def byGuid(guid: UUID): NotFoundError =
        NotFoundError(guid = Option(guid), message = s"Credential schema record cannot be found by `guid`=$guid")
      def byId(id: UUID): NotFoundError =
        NotFoundError(id = Option(id), message = s"Credential schema record cannot be found by `id`=$id")
    }

    final case class UpdateError(id: UUID, version: String, author: String, message: String) extends Error

    final case class UnexpectedError(msg: String) extends Error

    final case class CredentialSchemaValidationError(cause: CredentialSchemaError) extends Error
  }
}
