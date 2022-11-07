package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.agent.walletapi.model.ECPrivateKey
import io.iohk.atala.agent.walletapi.model.ECSignatures.ECSignature
import io.iohk.atala.prism.crypto.EC
import zio.*

object ECWrapper {

  /** @param data
    *   byte array to be signed
    * @param privateKey
    *   private key to be used for signing
    * @return
    *   a valid ECDSA signature for the given byte array using the given private key
    */
  def signBytesECDSA(data: Array[Byte], privateKey: ECPrivateKey): ECSignature = {
    val curve = EllipticCurve.SECP256K1
    val prism14PrivateKey = EC.INSTANCE.toPrivateKeyFromBytes(privateKey.toPaddedByteArray(curve))
    val prism14Signature = EC.INSTANCE.signBytes(data, prism14PrivateKey)
    ECSignature.fromPrism14Signature(prism14Signature)
  }

}
