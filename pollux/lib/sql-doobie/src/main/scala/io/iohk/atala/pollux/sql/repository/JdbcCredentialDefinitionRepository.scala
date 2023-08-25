package io.iohk.atala.pollux.sql.repository

import doobie.*
import doobie.implicits.*
import io.iohk.atala.pollux.core.model.schema.CredentialDefinition
import io.iohk.atala.pollux.core.repository.CredentialDefinitionRepository
import io.iohk.atala.pollux.core.repository.Repository
import io.iohk.atala.pollux.core.repository.Repository.*
import io.iohk.atala.pollux.sql.model.db.CredentialDefinition as CredentialDefinitionRow
import io.iohk.atala.pollux.sql.model.db.CredentialDefinitionSql
import zio.*
import zio.interop.catz.*

import java.util.UUID

class JdbcCredentialDefinitionRepository(xa: Transactor[Task]) extends CredentialDefinitionRepository[Task] {
  import CredentialDefinitionSql.*

  override def create(cd: CredentialDefinition): Task[CredentialDefinition] = {
    CredentialDefinitionSql
      .insert(CredentialDefinitionRow.fromModel(cd))
      .transact(xa)
      .map(CredentialDefinitionRow.toModel)
  }

  override def getByGuid(guid: UUID): Task[Option[CredentialDefinition]] = {
    CredentialDefinitionSql
      .findByGUID(guid)
      .transact(xa)
      .map(
        _.headOption
          .map(CredentialDefinitionRow.toModel)
      )
  }

  override def update(cd: CredentialDefinition): Task[Option[CredentialDefinition]] = {
    CredentialDefinitionSql
      .update(CredentialDefinitionRow.fromModel(cd))
      .transact(xa)
      .map(Option.apply)
      .map(_.map(CredentialDefinitionRow.toModel))
  }

  def getAllVersions(id: UUID, author: String): Task[Seq[String]] = {
    CredentialDefinitionSql
      .getAllVersions(id, author)
      .transact(xa)
  }

  override def delete(guid: UUID): Task[Option[CredentialDefinition]] = {
    CredentialDefinitionSql
      .delete(guid)
      .transact(xa)
      .map(Option.apply)
      .map(_.map(CredentialDefinitionRow.toModel))
  }

  def deleteAll(): Task[Long] = {
    CredentialDefinitionSql.deleteAll
      .transact(xa)
  }

  override def search(query: SearchQuery[CredentialDefinition.Filter]): Task[SearchResult[CredentialDefinition]] = {
    for {
      filteredRows <- CredentialDefinitionSql
        .lookup(
          authorOpt = query.filter.author,
          nameOpt = query.filter.name,
          versionOpt = query.filter.version,
          tagOpt = query.filter.tag,
          offset = query.skip,
          limit = query.limit
        )
        .transact(xa)
      entries = filteredRows.map(CredentialDefinitionRow.toModel)

      filteredRowsCount <- CredentialDefinitionSql
        .lookupCount(
          authorOpt = query.filter.author,
          nameOpt = query.filter.name,
          versionOpt = query.filter.version,
          tagOpt = query.filter.tag
        )
        .transact(xa)

      totalRowsCount <- CredentialDefinitionSql.totalCount.transact(xa)
    } yield SearchResult(entries, filteredRowsCount, totalRowsCount)
  }
}

object JdbcCredentialDefinitionRepository {
  val layer: URLayer[Transactor[Task], JdbcCredentialDefinitionRepository] =
    ZLayer.fromFunction(JdbcCredentialDefinitionRepository(_))
}
