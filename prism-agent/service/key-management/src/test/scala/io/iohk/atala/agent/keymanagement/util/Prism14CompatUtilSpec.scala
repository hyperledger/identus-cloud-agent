package io.iohk.atala.agent.keymanagement.util

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.agent.keymanagement.util.Prism14CompatUtil.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

object Prism14CompatUtilSpec extends ZIOSpecDefault {

  override def spec = suite("Prism14CompatUtil")(
    test("scala to kotlin and back for zero") {
      val bigInt1 = BigInt(0)
      val bigInt2 = bigInt1.toKotlinBigInt.toScalaBigInt
      assert(bigInt1)(equalTo(bigInt2))
    },
    test("kotlin to scala and back for zero") {
      val bigInt1 = BigInteger.Companion.fromLong(0)
      val bigInt2 = bigInt1.toScalaBigInt.toKotlinBigInt
      assert(bigInt1)(equalTo(bigInt2))
    },
    test("arbitrary bytearray should remain unchanged after multiple conversions") {
      val bytesGen = Gen.listOfBounded(1, 32)(Gen.byte)
      check(bytesGen) { bytes =>
        val bytes2 = BigInt(bytes.toArray).toKotlinBigInt.toScalaBigInt.toKotlinBigInt.toScalaBigInt.toByteArray.toList
        assert(bytes)(equalTo(bytes2))
      }
    }
  ) @@ TestAspect.samples(20)

}
