package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.schema.CredentialDefinition
import io.iohk.atala.pollux.core.repository.Repository.SearchCapability

import java.util.UUID

trait CredentialDefinitionRepository[F[_]]
    extends Repository[F, CredentialDefinition]
    with SearchCapability[F, CredentialDefinition.Filter, CredentialDefinition] {
  def create(cs: CredentialDefinition): F[CredentialDefinition]

  def getByGuid(guid: UUID): F[Option[CredentialDefinition]]

  def update(cs: CredentialDefinition): F[Option[CredentialDefinition]]

  def getAllVersions(id: UUID, author: String): F[Seq[String]]

  def delete(guid: UUID): F[Option[CredentialDefinition]]

  def deleteAll(): F[Long]
}
