package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.schema.CredentialSchema
import io.iohk.atala.pollux.core.model.schema.CredentialSchema.*
import io.iohk.atala.pollux.core.repository.Repository.SearchCapability
import io.iohk.atala.shared.models.WalletAccessContext
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
