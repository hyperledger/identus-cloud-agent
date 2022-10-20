package io.iohk.atala.agent.keymanagement.storage
import io.iohk.atala.castor.core.model.did.{PrismDID, PublishedDIDOperation}
import zio.*

private[keymanagement] class InMemoryDIDNonSecretStorage private (
    store: Ref[Map[PrismDID, PublishedDIDOperation.Create]]
) extends DIDNonSecretStorage {

  override def getCreatedDID(did: PrismDID): Task[Option[PublishedDIDOperation.Create]] = store.get.map(_.get(did))

  override def saveCreatedDID(did: PrismDID, createOp: PublishedDIDOperation.Create): Task[Unit] =
    store.update(_.updated(did, createOp))

  override def listCreatedDID: Task[Seq[PrismDID]] = store.get.map(_.toSeq.map(_._1))

}

private[keymanagement] object InMemoryDIDNonSecretStorage {

  val layer: ULayer[DIDNonSecretStorage] = {
    ZLayer.fromZIO(
      Ref.make(Map.empty[PrismDID, PublishedDIDOperation.Create]).map(InMemoryDIDNonSecretStorage(_))
    )
  }

}
