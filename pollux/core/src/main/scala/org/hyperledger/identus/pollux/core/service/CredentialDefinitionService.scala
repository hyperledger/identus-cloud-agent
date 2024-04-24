package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError
import org.hyperledger.identus.pollux.core.model.schema.CredentialDefinition
import org.hyperledger.identus.pollux.core.model.schema.CredentialDefinition.*
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.{IO, ZIO}

import java.util.UUID

trait CredentialDefinitionService {
  type Result[T] = ZIO[WalletAccessContext, CredentialDefinitionService.Error, T]

  /** @param in
    *   CredentialDefinition form for creating the instance
    * @return
    *   Created instance of the Credential Definition
    */
  def create(in: Input): Result[CredentialDefinition]

  /** @param guid
    *   Globally unique UUID of the credential definition
    * @return
    *   The instance of the credential definition or credential service error
    */
  def getByGUID(guid: UUID): IO[CredentialDefinitionService.Error, CredentialDefinition]

  def delete(guid: UUID): Result[CredentialDefinition]

  def lookup(filter: Filter, skip: Int, limit: Int): Result[FilteredEntries]
}

object CredentialDefinitionService {
  sealed trait Error

  object Error {
    def apply(throwable: Throwable): Error = RepositoryError(throwable)

    final case class RepositoryError(cause: Throwable) extends Error

    final case class NotFoundError(guid: Option[UUID] = None, id: Option[UUID] = None, message: String) extends Error

    object NotFoundError {
      def byGuid(guid: UUID): NotFoundError =
        NotFoundError(guid = Option(guid), message = s"Credential Definition record cannot be found by `guid`=$guid")

      def byId(id: UUID): NotFoundError =
        NotFoundError(id = Option(id), message = s"Credential Definition record cannot be found by `id`=$id")
    }

    final case class UpdateError(id: UUID, version: String, author: String, message: String) extends Error

    final case class UnexpectedError(msg: String) extends Error

    final case class CredentialDefinitionValidationError(cause: CredentialSchemaError) extends Error

    final case class CredentialDefinitionCreationError(msg: String) extends Error
  }
}
