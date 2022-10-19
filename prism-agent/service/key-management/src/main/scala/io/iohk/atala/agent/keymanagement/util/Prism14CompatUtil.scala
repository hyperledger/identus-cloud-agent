package io.iohk.atala.agent.keymanagement.util

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.util.ConversionUtilsKt

object Prism14CompatUtil {

  extension (n: BigInteger) {
    def toScalaBigInt: BigInt = BigInt(ConversionUtilsKt.toTwosComplementByteArray(n))
  }

  extension (n: BigInt) {
    def toKotlinBigInt: BigInteger = ConversionUtilsKt.fromTwosComplementByteArray(BigInteger.Companion, n.toByteArray)
  }

}
