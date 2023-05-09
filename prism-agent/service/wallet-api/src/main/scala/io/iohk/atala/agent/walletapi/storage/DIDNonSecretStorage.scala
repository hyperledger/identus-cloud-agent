package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.{DIDUpdateLineage, ManagedDIDState}
import io.iohk.atala.castor.core.model.did.{PrismDID, ScheduledDIDOperationStatus}
import zio.*

private[walletapi] trait DIDNonSecretStorage {

  def getManagedDIDState(did: PrismDID): Task[Option[ManagedDIDState]]

  def setManagedDIDState(did: PrismDID, state: ManagedDIDState): Task[Unit]

  /** Return a list of Managed DID as well as a count of all filtered items */
  def listManagedDID(offset: Option[Int], limit: Option[Int]): Task[(Seq[(PrismDID, ManagedDIDState)], Int)]

  def insertDIDUpdateLineage(did: PrismDID, updateLineage: DIDUpdateLineage): Task[Unit]

  def listUpdateLineage(did: Option[PrismDID], status: Option[ScheduledDIDOperationStatus]): Task[Seq[DIDUpdateLineage]]

  def setDIDUpdateLineageStatus(operationId: Array[Byte], status: ScheduledDIDOperationStatus): Task[Unit]

}
