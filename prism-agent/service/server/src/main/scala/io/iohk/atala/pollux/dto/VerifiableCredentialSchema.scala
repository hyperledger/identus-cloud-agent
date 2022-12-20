package io.iohk.atala.pollux.dto

import io.getquill.{Literal, SqlMirrorContext}
import io.getquill.doobie.DoobieContext

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
)

object VerifiableCredentialSchema {
  object sql extends DoobieContext.Postgres(Literal) {

    import io.getquill._
    import io.getquill.idiom._

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

    def deleteAll() = run {
      quote {
        query[VerifiableCredentialSchema].delete
      }
    }

    def lookup(
        authorOpt: Option[String] = None,
        nameOpt: Option[String] = None,
        versionOpt: Option[String] = None,
        tagOpt: Option[String] = None,
        offsetOpt: Option[Int] = Some(0),
        limitOpt: Option[Int] = Some(100)
    ) = {
      dynamicQuery[VerifiableCredentialSchema]
        .filterOpt(authorOpt)((vcs, author) => quote(vcs.author.like(author)))
        .filterOpt(nameOpt)((vcs, name) => quote(vcs.name.like(name)))
        .filterOpt(versionOpt)((vcs, version) => quote(vcs.version.like(version)))
        .filter(vcs => tagOpt.fold(quote(true))(tag => quote(vcs.tags.contains(lift(tag)))))
        .dropOpt(offsetOpt)
        .takeOpt(limitOpt)
    }
  }
}
