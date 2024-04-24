package org.hyperledger.identus.pollux.core.repository

import org.hyperledger.identus.pollux.core.model.schema.CredentialSchema
import org.hyperledger.identus.pollux.core.model.schema.CredentialSchema.*
import org.hyperledger.identus.pollux.core.repository.Repository.SearchCapability
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.{RIO, Task}

import java.util.UUID

trait CredentialSchemaRepository
    extends Repository[WalletTask, CredentialSchema]
    with SearchCapability[WalletTask, CredentialSchema.Filter, CredentialSchema] {
  def create(cs: CredentialSchema): RIO[WalletAccessContext, CredentialSchema]

  def getByGuid(guid: UUID): Task[Option[CredentialSchema]]

  def update(cs: CredentialSchema): RIO[WalletAccessContext, Option[CredentialSchema]]

  def getAllVersions(id: UUID, author: String): RIO[WalletAccessContext, Seq[String]]

  def delete(guid: UUID): RIO[WalletAccessContext, Option[CredentialSchema]]

  def deleteAll(): RIO[WalletAccessContext, Long]
}
