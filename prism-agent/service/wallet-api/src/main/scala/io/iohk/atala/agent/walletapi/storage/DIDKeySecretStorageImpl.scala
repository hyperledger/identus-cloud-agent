package io.iohk.atala.agent.walletapi.storage

import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.json.ast.Json.*

class DIDKeySecretStorageImpl(didSecretStorage: DIDSecretStorage) extends DIDKeySecretStorage {

  private val schemaId = "jwk"

  def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): RIO[WalletAccessContext, Int] = {
    val didSecret = keyPair.toJSONString
      .fromJson[Json]
      .map(json => DIDSecret(json, schemaId))
      .getOrElse(throw new RuntimeException("Unexpected Serialisation Failure"))
    didSecretStorage.insertKey(did, keyId, didSecret)
  }

  def getKey(did: DidId, keyId: String): RIO[WalletAccessContext, Option[OctetKeyPair]] = {
    for {
      maybeDidSecret <- didSecretStorage.getKey(did, keyId, schemaId)
    } yield maybeDidSecret.map(didSecret => OctetKeyPair.parse(didSecret.json.toString()))
  }
}

object DIDKeySecretStorageImpl {
  val layer: URLayer[DIDSecretStorage, DIDKeySecretStorage] =
    ZLayer.fromFunction(new DIDKeySecretStorageImpl(_))
}
