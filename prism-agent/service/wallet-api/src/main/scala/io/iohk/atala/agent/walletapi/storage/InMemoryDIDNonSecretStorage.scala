package io.iohk.atala.agent.walletapi.storage
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.castor.core.model.did.{PrismDID, PrismDIDOperation}
import zio.*

private[walletapi] class InMemoryDIDNonSecretStorage private (
    store: Ref[Map[PrismDID, ManagedDIDState]]
) extends DIDNonSecretStorage {

  override def getManagedDIDState(did: PrismDID): Task[Option[ManagedDIDState]] = store.get.map(_.get(did))

  override def setManagedDIDState(did: PrismDID, state: ManagedDIDState): Task[Unit] =
    store.update(_.updated(did, state))

  override def listManagedDID: Task[Map[PrismDID, ManagedDIDState]] = store.get

}

private[walletapi] object InMemoryDIDNonSecretStorage {

  val layer: ULayer[DIDNonSecretStorage] = {
    ZLayer.fromZIO(
      Ref.make(Map.empty[PrismDID, ManagedDIDState]).map(InMemoryDIDNonSecretStorage(_))
    )
  }

}
