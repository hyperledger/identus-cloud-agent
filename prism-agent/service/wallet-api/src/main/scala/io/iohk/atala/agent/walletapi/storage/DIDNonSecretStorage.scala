package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.castor.core.model.did.{PrismDID, PrismDIDOperation}
import zio.*

private[walletapi] trait DIDNonSecretStorage {

  def getManagedDIDState(did: PrismDID): Task[Option[ManagedDIDState]]

  def setManagedDIDState(did: PrismDID, state: ManagedDIDState): Task[Unit]

  def listManagedDID: Task[Map[PrismDID, ManagedDIDState]]

}
