package io.iohk.atala.pollux.core.service

import zio.{Task, ZIO, ZLayer, IO}
import io.iohk.atala.pollux.core.model.CredentialSchema
import io.iohk.atala.pollux.core.model.CredentialSchema.*

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

  def update(in: Input): Result[CredentialSchema]

  def delete(id: UUID): Result[CredentialSchema]

  def lookup(filter: Filter, skip: Int, limit: Int): Result[FilteredEntries]
}

object CredentialSchemaService {
  sealed trait Error

  object Error {
    def apply(throwable: Throwable): Error = RepositoryError(throwable)

    final case class RepositoryError(cause: Throwable) extends Error

    final case class NotFoundError(guid: UUID) extends Error

    final case class UnexpectedError(msg: String) extends Error
  }
}
