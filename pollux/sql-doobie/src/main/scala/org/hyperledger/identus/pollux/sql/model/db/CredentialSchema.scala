package org.hyperledger.identus.pollux.sql.model.db

import io.getquill.*
import io.getquill.context.json.PostgresJsonExtensions
import io.getquill.doobie.DoobieContext
import io.getquill.idiom.*
import org.hyperledger.identus.pollux.core.model.schema.Schema
import org.hyperledger.identus.shared.models.WalletId

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

case class CredentialSchema(
    guid: UUID,
    id: UUID,
    name: String,
    version: String,
    author: String,
    authored: OffsetDateTime,
    tags: Seq[String],
    description: String,
    `type`: String,
    schema: JsonValue[Schema],
    walletId: WalletId
) {
  lazy val uniqueConstraintKey = author + name + version

  def withTruncatedTimestamp(unit: ChronoUnit = ChronoUnit.MICROS): CredentialSchema =
    copy(authored = authored.truncatedTo(unit))

}

object CredentialSchema {
  def fromModel(
      m: org.hyperledger.identus.pollux.core.model.schema.CredentialSchema,
      walletId: WalletId
  ): CredentialSchema =
    CredentialSchema(
      guid = m.guid,
      id = m.id,
      name = m.name,
      version = m.version,
      author = m.author,
      authored = m.authored,
      tags = m.tags,
      description = m.description,
      `type` = m.`type`,
      schema = JsonValue(m.schema),
      walletId = walletId
    )

  def toModel(
      db: CredentialSchema
  ): org.hyperledger.identus.pollux.core.model.schema.CredentialSchema = {
    org.hyperledger.identus.pollux.core.model.schema.CredentialSchema(
      guid = db.guid,
      id = db.id,
      name = db.name,
      version = db.version,
      author = db.author,
      authored = db.authored,
      tags = db.tags,
      description = db.description,
      `type` = db.`type`,
      schema = db.schema.value
    )
  }
}

object CredentialSchemaSql extends DoobieContext.Postgres(SnakeCase) with PostgresJsonExtensions {
  def insert(schema: CredentialSchema) = run {
    quote(
      query[CredentialSchema]
        .insertValue(lift(schema))
    ).returning(cs => cs)
  }

  def findByGUID(guid: UUID) = run {
    quote(query[CredentialSchema].filter(_.guid == lift(guid)).take(1))
  }

  def findByID(id: UUID) = run {
    quote(query[CredentialSchema].filter(_.id == lift(id)))
  }

  def getAllVersions(id: UUID, author: String) = run {
    quote(
      query[CredentialSchema]
        .filter(_.id == lift(id))
        .filter(_.author == lift(author))
        .sortBy(_.version)(ord = Ord.asc)
        .map(_.version)
    )
  }

  def update(schema: CredentialSchema) = run {
    quote {
      query[CredentialSchema]
        .filter(_.guid == lift(schema.guid))
        .updateValue(lift(schema))
        .returning(s => s)
    }
  }

  def delete(guid: UUID) = run {
    quote {
      query[CredentialSchema]
        .filter(_.guid == lift(guid))
        .delete
        .returning(cs => cs)
    }
  }

  def deleteAll = run {
    quote {
      query[CredentialSchema].delete
    }
  }

  def totalCount = run {
    quote {
      query[CredentialSchema].size
    }
  }

  def lookupCount(
      idOpt: Option[UUID] = None,
      authorOpt: Option[String] = None,
      nameOpt: Option[String] = None,
      versionOpt: Option[String] = None,
      tagOpt: Option[String] = None
  ) = run {
    val q =
      idOpt.fold(quote(query[CredentialSchema]))(id => quote(query[CredentialSchema].filter(cs => cs.id == lift(id))))

    q.dynamic
      .filterOpt(authorOpt)((cs, author) => quote(cs.author.like(author)))
      .filterOpt(nameOpt)((cs, name) => quote(cs.name.like(name)))
      .filterOpt(versionOpt)((cs, version) => quote(cs.version.like(version)))
      .filter(cs =>
        tagOpt
          .fold(quote(true))(tag => quote(cs.tags.contains(lift(tag))))
      )
      .size
  }

  def lookup(
      idOpt: Option[UUID] = None,
      authorOpt: Option[String] = None,
      nameOpt: Option[String] = None,
      versionOpt: Option[String] = None,
      tagOpt: Option[String] = None,
      offset: Int = 0,
      limit: Int = 1000
  ) = run {
    val q =
      idOpt.fold(quote(query[CredentialSchema]))(id => quote(query[CredentialSchema].filter(cs => cs.id == lift(id))))

    q.dynamic
      .filterOpt(authorOpt)((cs, author) => quote(cs.author.like(author)))
      .filterOpt(nameOpt)((cs, name) => quote(cs.name.like(name)))
      .filterOpt(versionOpt)((cs, version) => quote(cs.version.like(version)))
      .filter(cs =>
        tagOpt
          .fold(quote(true))(tag => quote(cs.tags.contains(lift(tag))))
      )
      .sortBy(cs => cs.id)
      .drop(offset)
      .take(limit)
  }
}
