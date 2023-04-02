package io.iohk.atala.pollux.vc.jwt

import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import io.circe.Json
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers

import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey

class ES256KSignerTest extends AnyFunSuite with Matchers {

  test("ES256KSigner should use BouncyCastleProviderSingleton") {
    val ecKey = ECKeyGenerator(Curve.SECP256K1).provider(BouncyCastleProviderSingleton.getInstance()).generate()
    val signer = new ES256KSigner(ecKey.toPrivateKey).signer
    val provider = signer.getJCAContext.getProvider
    provider mustBe a[BouncyCastleProvider]
  }
}
