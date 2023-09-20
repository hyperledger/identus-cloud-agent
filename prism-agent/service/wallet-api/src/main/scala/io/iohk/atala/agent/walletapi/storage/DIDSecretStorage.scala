package io.iohk.atala.agent.walletapi.storage

import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

/** A simple single-user DID key storage */
trait DIDSecretStorage {
  def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): RIO[WalletAccessContext, Int]
  def getKey(did: DidId, keyId: String): RIO[WalletAccessContext, Option[OctetKeyPair]]
}
