package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.error.CredentialDefinitionServiceError
import org.hyperledger.identus.pollux.core.model.schema.CredentialDefinition
import org.hyperledger.identus.pollux.core.model.schema.CredentialDefinition.*
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.{IO, ZIO}

import java.util.UUID

trait CredentialDefinitionService {
  private[service] type Result[T] = ZIO[WalletAccessContext, CredentialDefinitionServiceError, T]

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
  def getByGUID(guid: UUID): IO[CredentialDefinitionServiceError, CredentialDefinition]

  def delete(guid: UUID): Result[CredentialDefinition]

  def lookup(filter: Filter, skip: Int, limit: Int): Result[FilteredEntries]
}
