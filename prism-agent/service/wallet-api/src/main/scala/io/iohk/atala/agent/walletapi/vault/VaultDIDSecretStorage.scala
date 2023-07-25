package io.iohk.atala.agent.walletapi.vault

import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.models.WalletAccessContext
import scala.util.Failure
import scala.util.Try
import zio.*

class VaultDIDSecretStorage(vaultKV: VaultKVClient) extends DIDSecretStorage {

  override def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): RIO[WalletAccessContext, Int] = {
    val kv = encodeOctetKeyPair(keyPair)
    for {
      wallet <- ZIO.service[WalletAccessContext]
      path = peerDidKeyPath(wallet)(did, keyId)
      alreadyExist <- vaultKV.get(path).map(_.isDefined)
      _ <- vaultKV
        .set(path, kv)
        .when(!alreadyExist)
        .someOrFail(Exception(s"Secret on path $path alraedy exists."))
    } yield 1
  }

  override def getKey(did: DidId, keyId: String): RIO[WalletAccessContext, Option[OctetKeyPair]] = {
    for {
      wallet <- ZIO.service[WalletAccessContext]
      path = peerDidKeyPath(wallet)(did, keyId)
      keyPair <- vaultKV.get(path).flatMap {
        case Some(kv) => ZIO.fromTry(decodeOctetKeyPair(kv)).asSome
        case None     => ZIO.none
      }
    } yield keyPair
  }

  private def peerDidKeyPath(wallet: WalletAccessContext)(did: DidId, keyId: String): String = {
    s"secret/${wallet.walletId.toInt}/dids/peer/${did.value}/keys/$keyId"
  }

  private def encodeOctetKeyPair(keyPair: OctetKeyPair): Map[String, String] = {
    Map("jwk" -> keyPair.toJSONString())
  }

  private def decodeOctetKeyPair(kv: Map[String, String]): Try[OctetKeyPair] = {
    kv.get("jwk") match {
      case Some(jwk) => Try(OctetKeyPair.parse(jwk))
      case None      => Failure(Exception("A property 'jwk' is missing from KV data"))
    }
  }
}

object VaultDIDSecretStorage {
  def layer: URLayer[VaultKVClient, DIDSecretStorage] = ZLayer.fromFunction(VaultDIDSecretStorage(_))
}
