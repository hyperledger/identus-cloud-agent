package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.agent.walletapi.model.ECPrivateKey
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.prism.crypto.EC
import scala.util.Try

object ECWrapper {

  def signBytes(curve: EllipticCurve, bytes: Array[Byte], privateKey: ECPrivateKey): Try[Array[Byte]] = {
    curve match {
      case EllipticCurve.SECP256K1 =>
        Try {
          val prism14PrivateKey = EC.INSTANCE.toPrivateKeyFromBytes(privateKey.toPaddedByteArray(curve))
          val signature = EC.INSTANCE.signBytes(bytes, prism14PrivateKey)
          signature.getEncoded
        }
    }
  }

}
