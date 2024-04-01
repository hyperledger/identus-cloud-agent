package io.iohk.atala.shared.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.util.ConversionUtilsKt

object KmpCompatUtil {

  extension (n: BigInteger) {
    def toScalaBigInt: BigInt = BigInt(ConversionUtilsKt.toTwosComplementByteArray(n))
  }

  extension (n: BigInt) {
    def toKotlinBigInt: BigInteger = ConversionUtilsKt.fromTwosComplementByteArray(BigInteger.Companion, n.toByteArray)
  }

}
