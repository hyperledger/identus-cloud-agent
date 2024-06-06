package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError
import org.hyperledger.identus.pollux.core.model.schema.CredentialSchema
import org.hyperledger.identus.pollux.core.model.schema.CredentialSchema.*
import org.hyperledger.identus.shared.models.{Failure, StatusCode, WalletAccessContext}
import zio.{IO, ZIO}

import java.util.UUID
trait CredentialSchemaService {
  private[service] type Result[T] = ZIO[WalletAccessContext, CredentialSchemaService.Error, T]

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
  def getByGUID(guid: UUID): IO[CredentialSchemaService.Error, CredentialSchema]

  def update(id: UUID, in: Input): Result[CredentialSchema]

  def delete(id: UUID): Result[CredentialSchema]

  def lookup(filter: Filter, skip: Int, limit: Int): Result[FilteredEntries]
}

object CredentialSchemaService {
  sealed trait Error(
      val statusCode: StatusCode,
      val userFacingMessage: String
  ) extends Failure {
    override val namespace = "CredentialSchema"
  }

  final case class GuidNotFoundError(guid: UUID)
      extends Error(
        StatusCode.NotFound,
        s"Credential Schema record cannot be found by `guid`=$guid"
      )

  final case class UpdateError(id: UUID, version: String, author: String, message: String)
      extends Error(
        StatusCode.BadRequest,
        s"Credential schema update error: id=$id, version=$version, author=$author, msg=$message"
      )

  final case class CredentialSchemaValidationError(cause: CredentialSchemaError)
      extends Error(
        StatusCode.BadRequest,
        s"Credential Schema Validation Error=$cause"
      )
}
