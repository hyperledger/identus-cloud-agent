package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.PeerDIDRecord
import io.iohk.atala.mercury.model.DidId
import zio.Task

trait DIDNonSecretStorageUnprotected {

  def getPeerDIDRecord(did: DidId): Task[Option[PeerDIDRecord]]

}
