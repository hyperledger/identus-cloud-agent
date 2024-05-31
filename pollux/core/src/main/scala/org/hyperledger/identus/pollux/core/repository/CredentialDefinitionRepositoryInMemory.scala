package org.hyperledger.identus.pollux.core.repository

import org.hyperledger.identus.pollux.core.model.*
import org.hyperledger.identus.pollux.core.model.error.CredentialRepositoryError.*
import org.hyperledger.identus.pollux.core.model.schema.CredentialDefinition
import org.hyperledger.identus.shared.models.{WalletAccessContext, WalletId}
import zio.*

import java.util.UUID

class CredentialDefinitionRepositoryInMemory(
    walletRefs: Ref[Map[WalletId, Ref[Map[UUID, CredentialDefinition]]]]
) extends CredentialDefinitionRepository {

  private def walletStoreRef: URIO[WalletAccessContext, Ref[Map[UUID, CredentialDefinition]]] =
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      refs <- walletRefs.get
      maybeWalletRef = refs.get(walletId)
      walletRef <- maybeWalletRef
        .fold {
          for {
            ref <- Ref.make(Map.empty[UUID, CredentialDefinition])
            _ <- walletRefs.set(refs.updated(walletId, ref))
          } yield ref
        }(ZIO.succeed)
    } yield walletRef

  override def create(record: CredentialDefinition): RIO[WalletAccessContext, CredentialDefinition] = {
    for {
      storeRef <- walletStoreRef
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
      storeRefs <- walletRefs.get
      storeRefOption <- ZIO.filter(storeRefs.values)(storeRef => storeRef.get.map(_.contains(guid))).map(_.headOption)
      record <- storeRefOption match {
        case Some(storeRef) => storeRef.get.map(_.get(guid))
        case None           => ZIO.none
      }
    } yield record
  }

  override def update(cs: CredentialDefinition): RIO[WalletAccessContext, Option[CredentialDefinition]] = {
    for {
      storeRef <- walletStoreRef
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

  override def getAllVersions(id: UUID, author: String): RIO[WalletAccessContext, Seq[String]] = {
    for {
      storeRef <- walletStoreRef
      store <- storeRef.get
    } yield store.values
      .filter(credDef => credDef.id == id && credDef.author == author)
      .map(_.version)
      .toSeq
  }

  override def delete(guid: UUID): RIO[WalletAccessContext, Option[CredentialDefinition]] = {
    for {
      storeRef <- walletStoreRef
      store <- storeRef.get
      maybeRecord = store.get(guid)
      _ <- maybeRecord match {
        case Some(record) => storeRef.update(r => r - record.id)
        case None         => ZIO.unit
      }
    } yield maybeRecord
  }

  override def deleteAll(): RIO[WalletAccessContext, Long] = {
    for {
      storeRef <- walletStoreRef
      store <- storeRef.get
      deleted = store.size
      _ <- storeRef.update(Map.empty)
    } yield deleted.toLong
  }

  override def search(
      query: Repository.SearchQuery[CredentialDefinition.Filter]
  ): RIO[WalletAccessContext, Repository.SearchResult[CredentialDefinition]] = {
    walletStoreRef.flatMap { storeRef =>
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
}

object CredentialDefinitionRepositoryInMemory {
  val layer: ULayer[CredentialDefinitionRepository] = ZLayer.fromZIO(
    Ref
      .make(Map.empty[WalletId, Ref[Map[UUID, CredentialDefinition]]])
      .map(CredentialDefinitionRepositoryInMemory(_))
  )
}
