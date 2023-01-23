package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.{DIDUpdateLineage, ManagedDIDState}
import io.iohk.atala.castor.core.model.did.{PrismDID, PrismDIDOperation, ScheduledDIDOperationStatus}
import zio.*

private[walletapi] trait DIDNonSecretStorage {

  def getManagedDIDState(did: PrismDID): Task[Option[ManagedDIDState]]

  def setManagedDIDState(did: PrismDID, state: ManagedDIDState): Task[Unit]

  def listManagedDID: Task[Map[PrismDID, ManagedDIDState]]

  def insertDIDUpdateLineage(did: PrismDID, updateLineage: DIDUpdateLineage): Task[Unit]

  def listUpdateLineage(did: Option[PrismDID], status: Option[ScheduledDIDOperationStatus]): Task[Seq[DIDUpdateLineage]]

  def setDIDUpdateLineageStatus(operationId: Array[Byte], status: ScheduledDIDOperationStatus): Task[Unit]

}
