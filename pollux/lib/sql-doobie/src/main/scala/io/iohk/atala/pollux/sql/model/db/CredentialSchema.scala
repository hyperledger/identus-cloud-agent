package io.iohk.atala.pollux.sql.model.db

import io.getquill.{Literal, SnakeCase}
import io.getquill.doobie.DoobieContext
import io.iohk.atala.pollux.core.model.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import io.getquill.context.json.PostgresJsonExtensions
import io.getquill.JsonValue

import java.time.{OffsetDateTime, ZonedDateTime}
import java.util.UUID

import io.getquill.*
import io.getquill.idiom.*

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
    schema: JsonValue[Schema]
) {
  lazy val uniqueConstraintKey = author + name + version
}

object CredentialSchema {
  def fromModel(
      m: io.iohk.atala.pollux.core.model.CredentialSchema
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
      schema = JsonValue(m.schema)
    )

  def toModel(
      db: CredentialSchema
  ): io.iohk.atala.pollux.core.model.CredentialSchema = {
    io.iohk.atala.pollux.core.model.CredentialSchema(
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
