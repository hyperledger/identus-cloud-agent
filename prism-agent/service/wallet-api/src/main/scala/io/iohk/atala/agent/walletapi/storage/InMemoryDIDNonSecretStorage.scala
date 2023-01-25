package io.iohk.atala.agent.walletapi.storage
import io.iohk.atala.agent.walletapi.model.{DIDUpdateLineage, ManagedDIDState}
import io.iohk.atala.castor.core.model.did.{PrismDID, PrismDIDOperation, ScheduledDIDOperationStatus}
import zio.*
import scala.collection.immutable.ArraySeq

private[walletapi] class InMemoryDIDNonSecretStorage private (
    stateStore: Ref[Map[PrismDID, ManagedDIDState]],
    lineageStore: Ref[Map[PrismDID, Seq[DIDUpdateLineage]]]
) extends DIDNonSecretStorage {

  override def getManagedDIDState(did: PrismDID): Task[Option[ManagedDIDState]] = stateStore.get.map(_.get(did))

  override def setManagedDIDState(did: PrismDID, state: ManagedDIDState): Task[Unit] =
    stateStore.update(_.updated(did, state))

  override def listManagedDID: Task[Map[PrismDID, ManagedDIDState]] = stateStore.get

  override def insertDIDUpdateLineage(did: PrismDID, updateLineage: DIDUpdateLineage): Task[Unit] = {
    lineageStore.update { currentStore =>
      val currentLineage = currentStore.getOrElse(did, Nil)
      val newLineage = currentLineage.appended(updateLineage)
      currentStore.updated(did, newLineage)
    }
  }

  override def listUpdateLineage(
      did: Option[PrismDID],
      status: Option[ScheduledDIDOperationStatus]
  ): Task[Seq[DIDUpdateLineage]] = lineageStore.get.map(
    _.toSeq
      .flatMap { case (k, v) => v.map(k -> _) }
      .filter { case (k, _) => did.fold(true)(_ == k) }
      .filter { case (_, v) => status.fold(true)(_ == v.status) }
      .map(_._2)
  )

  override def setDIDUpdateLineageStatus(operationId: Array[Byte], status: ScheduledDIDOperationStatus): Task[Unit] =
    for {
      now <- Clock.instant
      _ <- lineageStore.update { currentStore =>
        val oid = ArraySeq.from(operationId)
        currentStore.toSeq
          .flatMap { case (k, v) => v.map(k -> _) }
          .map {
            case (k, v) if v.operationId == oid => k -> v.copy(status = status, updatedAt = now)
            case i                              => i
          }
          .groupMap(_._1)(_._2)
      }
    } yield ()
}

private[walletapi] object InMemoryDIDNonSecretStorage {

  val layer: ULayer[DIDNonSecretStorage] = {
    ZLayer.fromZIO {
      for {
        stateStore <- Ref.make(Map.empty[PrismDID, ManagedDIDState])
        lineageStore <- Ref.make(Map.empty[PrismDID, Seq[DIDUpdateLineage]])
      } yield InMemoryDIDNonSecretStorage(stateStore, lineageStore)
    }
  }

}
