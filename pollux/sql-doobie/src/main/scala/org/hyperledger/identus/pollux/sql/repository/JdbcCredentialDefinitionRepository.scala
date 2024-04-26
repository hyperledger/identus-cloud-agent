package org.hyperledger.identus.pollux.sql.repository

import doobie.*
import doobie.implicits.*
import org.hyperledger.identus.pollux.core.model.schema.CredentialDefinition
import org.hyperledger.identus.pollux.core.repository.Repository.*
import org.hyperledger.identus.pollux.core.repository.{CredentialDefinitionRepository, Repository}
import org.hyperledger.identus.pollux.sql.model.db.{
  CredentialDefinitionSql,
  CredentialDefinition as CredentialDefinitionRow
}
import org.hyperledger.identus.shared.db.ContextAwareTask
import org.hyperledger.identus.shared.db.Implicits.*
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*
import zio.interop.catz.*

import java.util.UUID

case class JdbcCredentialDefinitionRepository(xa: Transactor[ContextAwareTask], xb: Transactor[Task])
    extends CredentialDefinitionRepository {
  import CredentialDefinitionSql.*

  override def create(cd: CredentialDefinition): RIO[WalletAccessContext, CredentialDefinition] = {
    ZIO.serviceWithZIO[WalletAccessContext](ctx =>
      CredentialDefinitionSql
        .insert(CredentialDefinitionRow.fromModel(cd, ctx.walletId))
        .transactWallet(xa)
        .map(CredentialDefinitionRow.toModel)
    )
  }

  override def getByGuid(guid: UUID): Task[Option[CredentialDefinition]] = {
    CredentialDefinitionSql
      .findByGUID(guid)
      .transact(xb)
      .map(
        _.headOption
          .map(CredentialDefinitionRow.toModel)
      )
  }

  override def update(cd: CredentialDefinition): RIO[WalletAccessContext, Option[CredentialDefinition]] = {
    ZIO.serviceWithZIO[WalletAccessContext](ctx =>
      CredentialDefinitionSql
        .update(CredentialDefinitionRow.fromModel(cd, ctx.walletId))
        .transactWallet(xa)
        .map(Option.apply)
        .map(_.map(CredentialDefinitionRow.toModel))
    )
  }

  def getAllVersions(id: UUID, author: String): RIO[WalletAccessContext, Seq[String]] = {
    CredentialDefinitionSql
      .getAllVersions(id, author)
      .transactWallet(xa)
  }

  override def delete(guid: UUID): RIO[WalletAccessContext, Option[CredentialDefinition]] = {
    CredentialDefinitionSql
      .delete(guid)
      .transactWallet(xa)
      .map(Option.apply)
      .map(_.map(CredentialDefinitionRow.toModel))
  }

  def deleteAll(): RIO[WalletAccessContext, Long] = {
    CredentialDefinitionSql.deleteAll
      .transactWallet(xa)
  }

  override def search(
      query: SearchQuery[CredentialDefinition.Filter]
  ): RIO[WalletAccessContext, SearchResult[CredentialDefinition]] = {
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
        .transactWallet(xa)
      entries = filteredRows.map(CredentialDefinitionRow.toModel)

      filteredRowsCount <- CredentialDefinitionSql
        .lookupCount(
          authorOpt = query.filter.author,
          nameOpt = query.filter.name,
          versionOpt = query.filter.version,
          tagOpt = query.filter.tag
        )
        .transactWallet(xa)

      totalRowsCount <- CredentialDefinitionSql.totalCount.transactWallet(xa)
    } yield SearchResult(entries, filteredRowsCount, totalRowsCount)
  }
}

object JdbcCredentialDefinitionRepository {
  val layer: URLayer[Transactor[ContextAwareTask] & Transactor[Task], JdbcCredentialDefinitionRepository] =
    ZLayer.fromFunction(JdbcCredentialDefinitionRepository.apply)
}
