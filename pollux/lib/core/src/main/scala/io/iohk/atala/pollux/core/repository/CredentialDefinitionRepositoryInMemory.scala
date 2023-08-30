package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.error.CredentialRepositoryError.*
import io.iohk.atala.pollux.core.model.schema.CredentialDefinition
import zio.*

import java.util.UUID

class CredentialDefinitionRepositoryInMemory(
    storeRef: Ref[Map[UUID, CredentialDefinition]]
) extends CredentialDefinitionRepository[Task] {
  override def create(record: CredentialDefinition): Task[CredentialDefinition] = {
    for {
      _ <- for {
        store <- storeRef.get
        maybeRecord = store.values.find(_.id == record.guid)
        _ <- maybeRecord match
          case None        => ZIO.unit
          case Some(value) => ZIO.fail(UniqueConstraintViolation("Unique Constraint Violation on 'id'"))
      } yield ()
      _ <- storeRef.update(r => r + (record.guid -> record))
    } yield record
  }

  override def getByGuid(guid: UUID): Task[Option[CredentialDefinition]] = {
    for {
      store <- storeRef.get
      record = store.get(guid)
    } yield record
  }

  override def update(cs: CredentialDefinition): Task[Option[CredentialDefinition]] = {
    for {
      store <- storeRef.get
      maybeExisting = store.get(cs.id)
      _ <- maybeExisting match {
        case Some(existing) =>
          val updatedStore = store.updated(cs.id, cs)
          storeRef.set(updatedStore)
        case None => ZIO.unit
      }
    } yield maybeExisting
  }

  override def getAllVersions(id: UUID, author: String): Task[Seq[String]] = {
    storeRef.get.map { store =>
      store.values
        .filter(credDef => credDef.id == id && credDef.author == author)
        .map(_.version)
        .toSeq
    }
  }

  override def delete(guid: UUID): Task[Option[CredentialDefinition]] = {
    for {
      store <- storeRef.get
      maybeRecord = store.get(guid)
      _ <- maybeRecord match {
        case Some(record) => storeRef.update(r => r - record.id)
        case None         => ZIO.unit
      }
    } yield maybeRecord
  }

  override def deleteAll(): Task[Long] = {
    for {
      store <- storeRef.get
      deleted = store.size
      _ <- storeRef.update(Map.empty)
    } yield deleted.toLong
  }

  override def search(
      query: Repository.SearchQuery[CredentialDefinition.Filter]
  ): Task[Repository.SearchResult[CredentialDefinition]] = {
    storeRef.get.map { store =>
      val filtered = store.values.filter { credDef =>
        query.filter.author.forall(_ == credDef.author) &&
        query.filter.name.forall(_ == credDef.name) &&
        query.filter.version.forall(_ == credDef.version) &&
        query.filter.tag.forall(tag => credDef.tag == tag)
      }
      val paginated = filtered.slice(query.skip, query.skip + query.limit)
      Repository.SearchResult(paginated.toSeq, paginated.size, filtered.size)
    }
  }
}

object CredentialDefinitionRepositoryInMemory {
  val layer: ULayer[CredentialDefinitionRepository[Task]] = ZLayer.fromZIO(
    Ref
      .make(Map.empty[UUID, CredentialDefinition])
      .map(CredentialDefinitionRepositoryInMemory(_))
  )
}
