package io.iohk.atala.agent.walletapi.storage
import io.iohk.atala.castor.core.model.did.{PrismDID, PublishedDIDOperation}
import zio.*

private[walletapi] class InMemoryDIDNonSecretStorage private (
    createdDIDStore: Ref[Map[PrismDID, PublishedDIDOperation.Create]],
    publishedDIDStore: Ref[Set[PrismDID]]
) extends DIDNonSecretStorage {

  override def getCreatedDID(did: PrismDID): Task[Option[PublishedDIDOperation.Create]] =
    createdDIDStore.get.map(_.get(did))

  override def saveCreatedDID(did: PrismDID, createOp: PublishedDIDOperation.Create): Task[Unit] =
    createdDIDStore.update(_.updated(did, createOp))

  override def listCreatedDID: Task[Seq[PrismDID]] = createdDIDStore.get.map(_.toSeq.map(_._1))

  override def savePublishedDID(did: PrismDID): Task[Unit] = publishedDIDStore.update(_.incl(did))

  override def listPublishedDID: Task[Seq[PrismDID]] = publishedDIDStore.get.map(_.toSeq)

}

private[walletapi] object InMemoryDIDNonSecretStorage {

  val layer: ULayer[DIDNonSecretStorage] = {
    ZLayer.fromZIO(
      for {
        createdDIDStore <- Ref.make(Map.empty[PrismDID, PublishedDIDOperation.Create])
        publishedDIDStore <- Ref.make(Set.empty)
      } yield InMemoryDIDNonSecretStorage(createdDIDStore, publishedDIDStore)
    )
  }

}
