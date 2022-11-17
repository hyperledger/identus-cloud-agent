package io.iohk.atala.agent.walletapi.storage
import io.iohk.atala.castor.core.model.did.{PrismDID, PrismDIDOperation}
import zio.*

private[walletapi] class InMemoryDIDNonSecretStorage private (
    store: Ref[Map[PrismDID, PrismDIDOperation.Create]]
) extends DIDNonSecretStorage {

  override def getCreatedDID(did: PrismDID): Task[Option[PrismDIDOperation.Create]] = store.get.map(_.get(did))

  override def saveCreatedDID(did: PrismDID, createOp: PrismDIDOperation.Create): Task[Unit] =
    store.update(_.updated(did, createOp))

  override def listCreatedDID: Task[Seq[PrismDID]] = store.get.map(_.toSeq.map(_._1))

}

private[walletapi] object InMemoryDIDNonSecretStorage {

  val layer: ULayer[DIDNonSecretStorage] = {
    ZLayer.fromZIO(
      Ref.make(Map.empty[PrismDID, PrismDIDOperation.Create]).map(InMemoryDIDNonSecretStorage(_))
    )
  }

}
