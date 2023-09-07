package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.schema.CredentialDefinition
import io.iohk.atala.pollux.core.repository.Repository.SearchCapability
import io.iohk.atala.shared.models.WalletAccessContext
import zio.{RIO, Task}

import java.util.UUID

trait CredentialDefinitionRepository
    extends Repository[WalletTask, CredentialDefinition]
    with SearchCapability[WalletTask, CredentialDefinition.Filter, CredentialDefinition] {
  def create(cs: CredentialDefinition): RIO[WalletAccessContext, CredentialDefinition]

  def getByGuid(guid: UUID): Task[Option[CredentialDefinition]]

  def update(cs: CredentialDefinition): RIO[WalletAccessContext, Option[CredentialDefinition]]

  def getAllVersions(id: UUID, author: String): RIO[WalletAccessContext, Seq[String]]

  def delete(guid: UUID): RIO[WalletAccessContext, Option[CredentialDefinition]]

  def deleteAll(): RIO[WalletAccessContext, Long]
}
