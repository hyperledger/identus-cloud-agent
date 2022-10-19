package io.iohk.atala.agent.keymanagement.storage

import io.iohk.atala.castor.core.model.did.{PrismDID, PublishedDIDOperation}
import zio.*

private[keymanagement] trait DIDNonSecretStorage {

  def saveCreatedDID(did: PrismDID, createOp: PublishedDIDOperation.Create): Task[Unit]

  def listCreatedDID: Task[Seq[PrismDID]]

}
