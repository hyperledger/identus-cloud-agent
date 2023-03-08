package io.iohk.atala.pollux.core.service
import io.iohk.atala.pollux.core.model.CredentialSchema
import io.iohk.atala.pollux.core.model.CredentialSchema.FilteredEntries
import io.iohk.atala.pollux.core.repository.CredentialSchemaRepository
import io.iohk.atala.pollux.core.repository.Repository.SearchQuery
import io.iohk.atala.pollux.core.service.CredentialSchemaService.Error.*
import zio.ZIO.getOrFailWith
import zio.{Task, URLayer, ZLayer}

import java.util.UUID

class CredentialSchemaServiceImpl(
    credentialSchemaRepository: CredentialSchemaRepository[Task]
) extends CredentialSchemaService {
  override def create(in: CredentialSchema.Input): Result[CredentialSchema] = {
    for {
      credentialSchema <- CredentialSchema.make(in)
      createdCredentialSchema <- credentialSchemaRepository
        .create(credentialSchema)
    } yield createdCredentialSchema
  }.mapError(t => RepositoryError(t))

  override def getByGUID(guid: UUID): Result[CredentialSchema] = {
    credentialSchemaRepository
      .getByGuid(guid)
      .mapError[CredentialSchemaService.Error](t => RepositoryError(t))
      .flatMap(
        getOrFailWith(NotFoundError(guid))(_)
      )
  }

  def getBy(
      author: String,
      id: UUID,
      version: String
  ): Result[CredentialSchema] = {
    getByGUID(CredentialSchema.makeGUID(author, id, version))
  }

  // TODO: Implement a business logic for a real schema update
  override def update(in: CredentialSchema.Input): Result[CredentialSchema] = {
    for {
      cs <- CredentialSchema.make(in)
      updated_opt <- credentialSchemaRepository
        .update(cs)
        .mapError[CredentialSchemaService.Error](RepositoryError.apply)
      updated <- getOrFailWith(NotFoundError(cs.guid))(updated_opt)
    } yield updated
  }
  override def delete(guid: UUID): Result[CredentialSchema] = {
    for {
      deleted_row_opt <- credentialSchemaRepository
        .delete(guid)
        .mapError(RepositoryError.apply)
      deleted_row <- getOrFailWith(NotFoundError(guid))(deleted_row_opt)
    } yield deleted_row
  }

  override def lookup(
      filter: CredentialSchema.Filter,
      skip: Int,
      limit: Int
  ): Result[CredentialSchema.FilteredEntries] = {
    credentialSchemaRepository
      .search(SearchQuery(filter, skip, limit))
      .mapError(t => RepositoryError(t))
      .map(sr => FilteredEntries(sr.entries, sr.count.toInt, sr.totalCount.toInt))
  }
}

object CredentialSchemaServiceImpl {
  val layer: URLayer[CredentialSchemaRepository[Task], CredentialSchemaService] =
    ZLayer.fromFunction(CredentialSchemaServiceImpl(_))
}
