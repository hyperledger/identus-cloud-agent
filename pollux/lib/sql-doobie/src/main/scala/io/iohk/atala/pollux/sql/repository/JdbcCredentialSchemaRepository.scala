package io.iohk.atala.pollux.sql.repository

import doobie.*
import io.iohk.atala.pollux.core.model.schema.CredentialSchema
import io.iohk.atala.pollux.core.repository.Repository.*
import io.iohk.atala.pollux.core.repository.{CredentialSchemaRepository, Repository}
import io.iohk.atala.pollux.sql.model.db.{CredentialSchemaSql, CredentialSchema as CredentialSchemaRow}
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.db.Implicits.*
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import zio.interop.catz.*
import doobie.*
import doobie.implicits.*

import java.util.UUID

case class JdbcCredentialSchemaRepository(xa: Transactor[ContextAwareTask], xb: Transactor[Task])
    extends CredentialSchemaRepository {
  import CredentialSchemaSql.*
  override def create(cs: CredentialSchema): RIO[WalletAccessContext, CredentialSchema] = {
    ZIO.serviceWithZIO[WalletAccessContext](ctx =>
      CredentialSchemaSql
        .insert(CredentialSchemaRow.fromModel(cs, ctx.walletId))
        .transactWallet(xa)
        .map(CredentialSchemaRow.toModel)
    )
  }

  override def getByGuid(guid: UUID): Task[Option[CredentialSchema]] = {
    CredentialSchemaSql
      .findByGUID(guid)
      .transact(xb)
      .map(
        _.headOption
          .map(CredentialSchemaRow.toModel)
      )
  }

  override def update(cs: CredentialSchema): RIO[WalletAccessContext, Option[CredentialSchema]] = {
    ZIO.serviceWithZIO[WalletAccessContext](ctx =>
      CredentialSchemaSql
        .update(CredentialSchemaRow.fromModel(cs, ctx.walletId))
        .transactWallet(xa)
        .map(Option.apply)
        .map(_.map(CredentialSchemaRow.toModel))
    )
  }

  def getAllVersions(id: UUID, author: String): RIO[WalletAccessContext, Seq[String]] = {
    CredentialSchemaSql
      .getAllVersions(id, author)
      .transactWallet(xa)
  }

  override def delete(guid: UUID): RIO[WalletAccessContext, Option[CredentialSchema]] = {
    CredentialSchemaSql
      .delete(guid)
      .transactWallet(xa)
      .map(Option.apply)
      .map(_.map(CredentialSchemaRow.toModel))
  }

  def deleteAll(): RIO[WalletAccessContext, Long] = {
    CredentialSchemaSql.deleteAll
      .transactWallet(xa)
  }

  override def search(
      query: SearchQuery[CredentialSchema.Filter]
  ): RIO[WalletAccessContext, SearchResult[CredentialSchema]] = {
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
        .transactWallet(xa)
      entries = filteredRows.map(CredentialSchemaRow.toModel)

      filteredRowsCount <- CredentialSchemaSql
        .lookupCount(
          authorOpt = query.filter.author,
          nameOpt = query.filter.name,
          versionOpt = query.filter.version,
          tagOpt = query.filter.tags
        )
        .transactWallet(xa)

      totalRowsCount <- CredentialSchemaSql.totalCount.transactWallet(xa)
    } yield SearchResult(entries, filteredRowsCount, totalRowsCount)
  }
}

object JdbcCredentialSchemaRepository {
  val layer: URLayer[Transactor[ContextAwareTask] & Transactor[Task], CredentialSchemaRepository] =
    ZLayer.fromFunction(JdbcCredentialSchemaRepository.apply)
}
