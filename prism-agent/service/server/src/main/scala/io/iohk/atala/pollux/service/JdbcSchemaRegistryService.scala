package io.iohk.atala.pollux.service
import doobie.util.transactor.Transactor
import io.iohk.atala.api.http.model.{CollectionStats, Order, Pagination}
import io.iohk.atala.pollux.schema.model.{VerifiableCredentialSchema, VerifiableCredentialSchemaInput, VerifiableCredentialSchemaPage}
import zio.Task
import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.*
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import cats.Functor
import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource}
import cats.syntax.functor.*
import io.iohk.atala.pollux.sql.model.VerifiableCredentialSchema as VCS
import io.iohk.atala.pollux.service.SchemaRegistryService

import io.getquill.*
import io.getquill.idiom.*

import java.util.UUID
import scala.util.Try

class JdbcSchemaRegistryService(tx: Transactor[Task]) extends SchemaRegistryService {

  import io.iohk.atala.pollux.sql.model.VerifiableCredentialSchema.sql
  import io.iohk.atala.pollux.sql.model.VerifiableCredentialSchema.sql.*
  import JdbcSchemaRegistryService.given_Conversion_VerifiableCredentialSchema_VCS
  import JdbcSchemaRegistryService.given_Conversion_VCS_VerifiableCredentialSchema
  override def createSchema(in: VerifiableCredentialSchemaInput): Task[VerifiableCredentialSchema] = {
    for {
      vcs <- zio.ZIO.fromTry(Try(VerifiableCredentialSchema(in)))
      vcs_dto: VCS = vcs.convert
      action <- sql.insert(vcs_dto).transact(tx)
    } yield vcs
  }

  override def getSchemaById(id: UUID): Task[Option[VerifiableCredentialSchema]] = {
    for {
      vcsOption <- sql.findBy(id).transact(tx).map(_.headOption)
      result = vcsOption.map[VerifiableCredentialSchema](dto => dto.convert)
    } yield result
  }

  override def lookupSchemas(
      filter: VerifiableCredentialSchema.Filter,
      pagination: Pagination,
      order: Option[Order]
  ): Task[(VerifiableCredentialSchemaPage, CollectionStats)] = {
    val lookupQuery = sql.lookup(
      authorOpt = filter.author,
      nameOpt = filter.name,
      versionOpt = filter.version,
      attributeOpt = filter.attribute,
      tagOpt = filter.tags,
      offsetOpt = Some(pagination.offset),
      limitOpt = Some(pagination.limit)
    )
    val lookupQueryCount = sql.lookupCount(
      authorOpt = filter.author,
      nameOpt = filter.name,
      versionOpt = filter.version,
      attributeOpt = filter.attribute,
      tagOpt = filter.tags
    )

    val totalCountQuery = sql.totalCount

    for {
      dtoList <- lookupQuery.transact(tx)
      count <- lookupQueryCount.transact(tx)
      totalCount <- totalCountQuery.transact(tx)
      vcsList = dtoList.map[VerifiableCredentialSchema](dto => dto.convert)
    } yield (VerifiableCredentialSchemaPage(contents = vcsList), CollectionStats(totalCount, count))
  }

}

object JdbcSchemaRegistryService {

  val layer: URLayer[Transactor[Task], JdbcSchemaRegistryService] =
    ZLayer.fromFunction(new JdbcSchemaRegistryService(_))

  given scala.Conversion[VerifiableCredentialSchema, VCS] = vcs =>
    VCS(
      id = vcs.id,
      name = vcs.name,
      version = vcs.version,
      tags = vcs.tags,
      description = vcs.description,
      attributes = vcs.attributes,
      author = vcs.author,
      authored = vcs.authored
    )

  given scala.Conversion[VCS, VerifiableCredentialSchema] = vcs =>
    VerifiableCredentialSchema(
      id = vcs.id,
      name = vcs.name,
      version = vcs.version,
      tags = vcs.tags,
      description = vcs.description,
      attributes = vcs.attributes,
      author = vcs.author,
      authored = vcs.authored,
      proof = None
    )

}
