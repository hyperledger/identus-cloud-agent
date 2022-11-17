package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.castor.core.model.did.{PrismDID, PrismDIDOperation}
import zio.*

private[walletapi] trait DIDNonSecretStorage {

  def getCreatedDID(did: PrismDID): Task[Option[PrismDIDOperation.Create]]

  def saveCreatedDID(did: PrismDID, createOp: PrismDIDOperation.Create): Task[Unit]

  def listCreatedDID: Task[Seq[PrismDID]]

}
