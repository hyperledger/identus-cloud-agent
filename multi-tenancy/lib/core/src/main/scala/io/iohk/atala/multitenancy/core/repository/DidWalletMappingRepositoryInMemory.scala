package io.iohk.atala.multitenancy.core.repository

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.multitenancy.core.model.DidWalletMappingRecord
import io.iohk.atala.multitenancy.core.model.error.DidWalletMappingRepositoryError.*
import io.iohk.atala.shared.models.WalletId
import zio.*

class DidWalletMappingRepositoryInMemory(storeRef: Ref[Map[DidId, DidWalletMappingRecord]])
    extends DidWalletMappingRepository[Task] {

  override def deleteDidWalletMappingByDid(didId: DidId): Task[Int] = {
    for {
      maybeRecord <- getDidWalletMappingByDid(didId)
      count <- maybeRecord
        .map(_ =>
          for {
            _ <- storeRef.update(r => r.removed(didId))
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def deleteDidWalletMappingByWalletId(walletId: WalletId): Task[Int] = {
    for {
      store <- storeRef.get
      count <- {
        val records = store.values
          .filter(_.walletId == walletId)
          .map(_.did)

        for {
          _ <- storeRef.update(r => r.removedAll(records))
        } yield records.size
      }
    } yield count
  }

  override def getDidWalletMappingByWalletId(walletId: WalletId): Task[Seq[DidWalletMappingRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values.filter(_.walletId == walletId).toSeq
  }

  override def getDidWalletMappingRecords: Task[Seq[DidWalletMappingRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values.toSeq
  }

  override def getDidWalletMappingByDid(did: DidId): Task[Option[DidWalletMappingRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values.find(_.did == did)
  }

  override def createDidWalletMappingRecord(record: DidWalletMappingRecord): Task[Int] = {
    for {
      _ <- for {
        store <- storeRef.get
        maybeRecord <- ZIO.succeed(store.values.find(_.did == record.did))
        _ <- maybeRecord match
          case None        => ZIO.unit
          case Some(value) => ZIO.fail(UniqueConstraintViolation("Unique Constraint Violation on 'did'"))
      } yield ()
      _ <- storeRef.update(r => r + (record.did -> record))
    } yield 1
  }

}

object DidWalletMappingRepositoryInMemory {
  val layer: ULayer[DidWalletMappingRepository[Task]] = ZLayer.fromZIO(
    Ref
      .make(Map.empty[DidId, DidWalletMappingRecord])
      .map(DidWalletMappingRepositoryInMemory(_))
  )
}
