package io.iohk.atala.pollux.sql.model

import io.getquill.doobie.DoobieContext
import io.getquill.{Literal, SnakeCase, SqlMirrorContext}

import java.time.{OffsetDateTime, ZonedDateTime}
import java.util.UUID

case class VerifiableCredentialSchema(
    id: UUID,
    name: String,
    version: String,
    tags: List[String],
    description: Option[String],
    attributes: List[String],
    author: String,
    authored: OffsetDateTime
) {
  lazy val uniqueConstraintKey = author + name + version
}

object VerifiableCredentialSchema {
  object sql extends DoobieContext.Postgres(SnakeCase) {

    import io.getquill.*
    import io.getquill.idiom.*

    def insert(schema: VerifiableCredentialSchema) = run {
      quote {
        query[VerifiableCredentialSchema].insertValue(lift(schema))
      }
    }

    def findBy(id: UUID) = run {
      quote {
        query[VerifiableCredentialSchema]
          .filter(schema => schema.id == lift(id))
          .take(1)
      }
    }

    def update(schema: VerifiableCredentialSchema) = run {
      quote {
        query[VerifiableCredentialSchema]
          .filter(s => s.id == lift(schema.id))
          .updateValue(lift(schema))
          .returning(s => s)
      }
    }

    def delete(id: UUID) = run {
      quote {
        query[VerifiableCredentialSchema]
          .filter(schema => schema.id == lift(id))
          .delete
          .returning(_.id)
      }
    }

    def deleteAll = run {
      quote {
        query[VerifiableCredentialSchema].delete
      }
    }

    def totalCount = run {
      quote {
        query[VerifiableCredentialSchema].size
      }
    }

    def lookup(
        authorOpt: Option[String] = None,
        nameOpt: Option[String] = None,
        versionOpt: Option[String] = None,
        attributeOpt: Option[String] = None,
        tagOpt: Option[String] = None,
        offsetOpt: Option[Int] = Some(0),
        limitOpt: Option[Int] = Some(100)
    ) = {
      val quotedQuery = limitOpt
        .fold(
          quote(query[VerifiableCredentialSchema]))(limit =>
          quote(query[VerifiableCredentialSchema].take(lift(limit)))
        )

      val dynamicQuery = quotedQuery.dynamic
        .filterOpt(authorOpt)((vcs, author) => quote(vcs.author.like(author)))
        .filterOpt(nameOpt)((vcs, name) => quote(vcs.name.like(name)))
        .filterOpt(versionOpt)((vcs, version) =>
          quote(vcs.version.like(version))
        )
        .filter(vcs =>
          attributeOpt
            .fold(quote(true))(attr =>
              quote(vcs.attributes.contains(lift(attr)))
            )
        )
        .filter(vcs =>
          tagOpt
            .fold(quote(true))(tag => quote(vcs.tags.contains(lift(tag))))
        )
        .dropOpt(offsetOpt)
        .sortBy(vcs => vcs.id)
      //.takeOpt(limitOpt) //This line pollutes the stdout

      dynamicQuery
    }
  }
}
