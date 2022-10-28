package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.castor.core.model.did.{PrismDID, PublishedDIDOperation}
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*

private[walletapi] trait DIDNonSecretStorage {

  def getCreatedDID(did: PrismDID): Task[Option[PublishedDIDOperation.Create]]

  def saveCreatedDID(did: PrismDID, createOp: PublishedDIDOperation.Create): Task[Unit]

  def listCreatedDID: Task[Seq[PrismDID]]

  def savePublishedDID(did: PrismDID): Task[Unit]

  def listPublishedDID: Task[Seq[PrismDID]]

  def upsertDIDVersion(did: PrismDID, version: HexString): Task[Unit]

  def getDIDVersion(did: PrismDID): Task[Option[HexString]]

}
