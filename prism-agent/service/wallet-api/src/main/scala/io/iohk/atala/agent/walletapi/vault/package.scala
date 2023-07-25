package io.iohk.atala.agent.walletapi

import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.shared.models.HexString
import scala.util.Failure
import scala.util.Try

package object vault {
  trait KVCodec[T] {
    def encode(value: T): Map[String, String]
    def decode(kv: Map[String, String]): Try[T]
  }

  given KVCodec[OctetKeyPair] = new {
    override def encode(value: OctetKeyPair): Map[String, String] = {
      Map("jwk" -> value.toJSONString())
    }

    override def decode(kv: Map[String, String]): Try[OctetKeyPair] = {
      kv.get("jwk") match {
        case Some(jwk) => Try(OctetKeyPair.parse(jwk))
        case None      => Failure(Exception("A property 'jwk' is missing from KV data"))
      }
    }
  }

  given KVCodec[WalletSeed] = new {
    override def encode(value: WalletSeed): Map[String, String] = {
      val bytes = value.toByteArray
      Map(
        "value" -> HexString.fromByteArray(bytes).toString
      )
    }

    override def decode(kv: Map[String, String]): Try[WalletSeed] = {
      kv.get("value") match {
        case None => Failure(Exception("A property 'value' is missing from KV data"))
        case Some(encodedSeed) =>
          HexString
            .fromString(encodedSeed)
            .map(_.toByteArray)
            .map(WalletSeed.fromByteArray)
      }
    }
  }
}
