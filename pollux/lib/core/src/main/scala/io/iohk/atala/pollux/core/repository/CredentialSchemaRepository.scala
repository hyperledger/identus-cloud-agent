package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.CredentialSchema
import io.iohk.atala.pollux.core.model.CredentialSchema.*
import io.iohk.atala.pollux.core.repository.Repository.SearchCapability
import java.util.UUID

trait CredentialSchemaRepository[F[_]]
    extends Repository[F, CredentialSchema]
    with SearchCapability[F, CredentialSchema.Filter, CredentialSchema] {
  def create(cs: CredentialSchema): F[CredentialSchema]

  def getByGuid(guid: UUID): F[Option[CredentialSchema]]

  def update(cs: CredentialSchema): F[Option[CredentialSchema]]

  def getAllVersions(id: UUID, author: String): F[Seq[String]]

  def delete(guid: UUID): F[Option[CredentialSchema]]

  def deleteAll(): F[Long]
}
