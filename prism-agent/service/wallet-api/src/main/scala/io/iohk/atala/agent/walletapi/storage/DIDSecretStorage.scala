package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import zio.json.ast.Json

case class DIDSecret(json: Json, schemaId: String)

/** A simple single-user DID key storage */
trait DIDSecretStorage {
  def insertKey(did: DidId, keyId: String, didSecret: DIDSecret): RIO[WalletAccessContext, Int]

  def getKey(did: DidId, keyId: String, schemaId: String): RIO[WalletAccessContext, Option[DIDSecret]]
}
