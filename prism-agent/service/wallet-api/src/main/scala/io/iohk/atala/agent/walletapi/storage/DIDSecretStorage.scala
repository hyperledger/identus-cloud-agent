package io.iohk.atala.agent.walletapi.storage

import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.crypto.jwk.FromJWK
import io.iohk.atala.shared.crypto.jwk.JWK
import io.iohk.atala.shared.models.WalletAccessContext
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
