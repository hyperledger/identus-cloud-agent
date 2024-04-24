package org.hyperledger.identus.agent.walletapi.storage

import com.nimbusds.jose.jwk.OctetKeyPair
import org.hyperledger.identus.mercury.model.DidId
import org.hyperledger.identus.castor.core.model.did.PrismDID
import org.hyperledger.identus.shared.crypto.jwk.FromJWK
import org.hyperledger.identus.shared.crypto.jwk.JWK
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

/** A simple single-user DID key storage */
trait DIDSecretStorage {
  def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): RIO[WalletAccessContext, Int]
  def getKey(did: DidId, keyId: String): RIO[WalletAccessContext, Option[OctetKeyPair]]

  def insertPrismDIDKeyPair[K](
      did: PrismDID,
      keyId: String,
      operationHash: Array[Byte],
      keyPair: K
  )(using c: Conversion[K, JWK]): URIO[WalletAccessContext, Unit]

  def getPrismDIDKeyPair[K](did: PrismDID, keyId: String, operationHash: Array[Byte])(using
      c: FromJWK[K]
  ): URIO[WalletAccessContext, Option[K]]
}
