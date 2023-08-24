package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.mercury.model.DidId
import zio.*
import zio.json.ast.Json

case class DIDSecret(json: Json, schemaId: String)

/** A simple single-user DID key storage */
trait DIDSecretStorage {
  def insertKey(did: DidId, keyId: String, didSecret: DIDSecret): Task[Int]

  def getKey(did: DidId, keyId: String, schemaId: String): Task[Option[DIDSecret]]
}
