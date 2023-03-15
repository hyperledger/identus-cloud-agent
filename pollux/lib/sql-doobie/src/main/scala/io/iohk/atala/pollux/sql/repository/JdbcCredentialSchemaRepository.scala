package io.iohk.atala.pollux.sql.repository

import cats.data.NonEmptyList
import cats.instances.seq
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.iohk.atala.pollux.core.model.CredentialSchema
import io.iohk.atala.pollux.core.repository.Repository.*
import io.iohk.atala.pollux.core.repository.{CredentialSchemaRepository, Repository}
import io.iohk.atala.pollux.sql.model.db.{CredentialSchemaSql, CredentialSchema as CredentialSchemaRow}
import zio.*
import zio.interop.catz.*

import java.util.UUID

class JdbcCredentialSchemaRepository(xa: Transactor[Task]) extends CredentialSchemaRepository[Task] {
  import CredentialSchemaSql.*
  override def create(cs: CredentialSchema): Task[CredentialSchema] = {
    CredentialSchemaSql
      .insert(CredentialSchemaRow.fromModel(cs))
      .transact(xa)
      .map(CredentialSchemaRow.toModel)
  }

  override def getByGuid(guid: UUID): Task[Option[CredentialSchema]] = {
    CredentialSchemaSql
      .findByGUID(guid)
      .transact(xa)
      .map(
        _.headOption
          .map(CredentialSchemaRow.toModel)
      )
  }

  override def update(cs: CredentialSchema): Task[Option[CredentialSchema]] = {
    CredentialSchemaSql
      .update(CredentialSchemaRow.fromModel(cs))
      .transact(xa)
      .map(Option.apply)
      .map(_.map(CredentialSchemaRow.toModel))
  }

  def getAllVersions(id: UUID, author: String): Task[Seq[String]] = {
    CredentialSchemaSql
      .getAllVersions(id, author)
      .transact(xa)
  }

  override def delete(guid: UUID): Task[Option[CredentialSchema]] = {
    CredentialSchemaSql
      .delete(guid)
      .transact(xa)
      .map(Option.apply)
      .map(_.map(CredentialSchemaRow.toModel))
  }

  def deleteAll(): Task[Long] = {
    CredentialSchemaSql.deleteAll
      .transact(xa)
  }

  override def search(query: SearchQuery[CredentialSchema.Filter]): Task[SearchResult[CredentialSchema]] = {
    for {
      filteredRows <- CredentialSchemaSql
        .lookup(
          authorOpt = query.filter.author,
          nameOpt = query.filter.name,
          versionOpt = query.filter.version,
          tagOpt = query.filter.tags,
          offset = query.skip,
          limit = query.limit
        )
        .transact(xa)
      entries = filteredRows.map(CredentialSchemaRow.toModel)

      filteredRowsCount <- CredentialSchemaSql
        .lookupCount(
          authorOpt = query.filter.author,
          nameOpt = query.filter.name,
          versionOpt = query.filter.version,
          tagOpt = query.filter.tags
        )
        .transact(xa)

      totalRowsCount <- CredentialSchemaSql.totalCount.transact(xa)
    } yield SearchResult(entries, filteredRowsCount, totalRowsCount)
  }
}

object JdbcCredentialSchemaRepository {
  val layer: URLayer[Transactor[Task], JdbcCredentialSchemaRepository] =
    ZLayer.fromFunction(JdbcCredentialSchemaRepository(_))
}
