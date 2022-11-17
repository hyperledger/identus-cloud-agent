package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.agent.walletapi.model.ECPrivateKey
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.agent.walletapi.util.Prism14CompatUtil.*
import scala.util.Try

object ECWrapper {

  def signBytes(curve: EllipticCurve, bytes: Array[Byte], privateKey: ECPrivateKey): Try[Array[Byte]] = {
    curve match {
      case EllipticCurve.SECP256K1 =>
        Try {
          val privateKeyBigInt = BigInt(privateKey.toPaddedByteArray(curve)).toKotlinBigInt
          val prism14PrivateKey = EC.INSTANCE.toPrivateKeyFromBigInteger(privateKeyBigInt)
          val signature = EC.INSTANCE.signBytes(bytes, prism14PrivateKey)
          signature.getEncoded
        }
    }
  }

}
