package io.iohk.atala.agent.walletapi.crypto

import zio.*
import zio.test.*
import zio.test.Assertion.*
import javafx.scene.shape.Ellipse
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.shared.models.HexString
import org.bouncycastle.jce.ECNamedCurveTable

object ApolloSpec extends ZIOSpecDefault {

  override def spec = {
    val tests = Seq(
      publicKeySpec,
      privateKeySpec,
      ecKeyFactorySpec,
    )
    suite("Apollo - Prism14 implementation")(tests: _*).provideLayer(Apollo.prism14Layer)
  }

  private val publicKeySpec = suite("ECPublicKey")(
    test("same public key bytes must be equal and have same hashCode") {
      for {
        apollo <- ZIO.service[Apollo]
        keyPair <- apollo.ecKeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        pk1 = keyPair.publicKey
        pk2 = apollo.ecKeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, pk1.encode).get
      } yield assert(pk1)(equalTo(pk2)) &&
        assert(pk1 == pk2)(isTrue) &&
        assert(pk1.hashCode())(equalTo(pk2.hashCode()))
    },
    test("different public key bytes must not be equal and have different hashCode") {
      for {
        apollo <- ZIO.service[Apollo]
        keyPair1 <- apollo.ecKeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        keyPair2 <- apollo.ecKeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        pk1 = keyPair1.publicKey
        pk2 = keyPair2.publicKey
      } yield assert(pk1)(not(equalTo(pk2))) &&
        assert(pk1 == pk2)(isFalse) &&
        assert(pk1.hashCode())(not(equalTo(pk2.hashCode())))
    },
    test("convert to java PublicKey class") {
      // priv: 0x2789649b57d8f5df144a817f660b494e7a86d465ba86a638a2b525884c5c5849
      // pub: 0x037b7e17f0524db221af0dd74bd21dec2fc6d0955bbfd43ec7d96ca61dbee2d9d1
      // x: 55857268325124588620525700020439091507381445732605907422424441486941792426449
      // y: 36684214325164537089180371592352190153822062261502257266280631050350493669941
      val compressed = "037b7e17f0524db221af0dd74bd21dec2fc6d0955bbfd43ec7d96ca61dbee2d9d1"
      val bytes = HexString.fromString(compressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        pk = apollo.ecKeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
        javaPk = pk.toJavaPublicKey
      } yield assert(javaPk.getAlgorithm())(equalTo("EC")) &&
        assert(javaPk.getW().getAffineX().toString())(
          equalTo("55857268325124588620525700020439091507381445732605907422424441486941792426449")
        ) &&
        assert(javaPk.getW().getAffineY().toString())(
          equalTo("36684214325164537089180371592352190153822062261502257266280631050350493669941")
        )
    },
    test("sign a message and verify using public key") {
      val message = BigInt("42").toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        keyPair <- apollo.ecKeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        privateKey = keyPair.privateKey
        publicKey = privateKey.computePublicKey
        signature = privateKey.sign(message).get
      } yield assert(publicKey.verify(message, signature))(isSuccess)
    },
    test("sign a message and verify using different public key should fail") {
      val message = BigInt("42").toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        keyPair1 <- apollo.ecKeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        keyPair2 <- apollo.ecKeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        privateKey = keyPair1.privateKey
        publicKey = keyPair2.publicKey
        signature = privateKey.sign(message).get
      } yield assert(publicKey.verify(message, signature))(isFailure)
    }
  )

  private val privateKeySpec = suite("ECPrivateKey")(
    test("same private key bytes must be equal and have same hashCode") {
      for {
        apollo <- ZIO.service[Apollo]
        keyPair <- apollo.ecKeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        pk1 = keyPair.privateKey
        pk2 = apollo.ecKeyFactory.privateKeyFromEncoded(EllipticCurve.SECP256K1, pk1.encode).get
      } yield assert(pk1)(equalTo(pk2)) &&
        assert(pk1 == pk2)(isTrue) &&
        assert(pk1.hashCode())(equalTo(pk2.hashCode()))
    },
    test("different private key bytes must not be equal and have different hashCode") {
      for {
        apollo <- ZIO.service[Apollo]
        keyPair1 <- apollo.ecKeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        keyPair2 <- apollo.ecKeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        pk1 = keyPair1.privateKey
        pk2 = keyPair2.privateKey
      } yield assert(pk1)(not(equalTo(pk2))) &&
        assert(pk1 == pk2)(isFalse) &&
        assert(pk1.hashCode())(not(equalTo(pk2.hashCode())))
    },
    test("convert to java PrivateKey class") {
      // priv: 0x2789649b57d8f5df144a817f660b494e7a86d465ba86a638a2b525884c5c5849
      // pub: 0x037b7e17f0524db221af0dd74bd21dec2fc6d0955bbfd43ec7d96ca61dbee2d9d1
      // x: 55857268325124588620525700020439091507381445732605907422424441486941792426449
      // y: 36684214325164537089180371592352190153822062261502257266280631050350493669941
      val compressed = "2789649b57d8f5df144a817f660b494e7a86d465ba86a638a2b525884c5c5849"
      val bytes = HexString.fromString(compressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        pk = apollo.ecKeyFactory.privateKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
        javaPk = pk.toJavaPrivateKey
      } yield assert(javaPk.getAlgorithm())(equalTo("EC")) &&
        assert(javaPk.getS().toByteArray())(equalTo(bytes))
    },
    test("compute public key from private key") {
      // priv: 0x2789649b57d8f5df144a817f660b494e7a86d465ba86a638a2b525884c5c5849
      // pub: 0x037b7e17f0524db221af0dd74bd21dec2fc6d0955bbfd43ec7d96ca61dbee2d9d1
      // x: 55857268325124588620525700020439091507381445732605907422424441486941792426449
      // y: 36684214325164537089180371592352190153822062261502257266280631050350493669941
      val compressed = "2789649b57d8f5df144a817f660b494e7a86d465ba86a638a2b525884c5c5849"
      val bytes = HexString.fromString(compressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        privateKey = apollo.ecKeyFactory.privateKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
        publicKey = privateKey.computePublicKey
        javaPk = publicKey.toJavaPublicKey
      } yield assert(javaPk.getAlgorithm())(equalTo("EC")) &&
        assert(javaPk.getW().getAffineX().toString())(
          equalTo("55857268325124588620525700020439091507381445732605907422424441486941792426449")
        ) &&
        assert(javaPk.getW().getAffineY().toString())(
          equalTo("36684214325164537089180371592352190153822062261502257266280631050350493669941")
        )
    }
  )

  private val ecKeyFactorySpec = suite("ECKeyFactory")(
    test("decode invalid public key should fail") {
      for {
        apollo <- ZIO.service[Apollo]
        decodeResult = apollo.ecKeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, Array.emptyByteArray)
      } yield assert(decodeResult)(isFailure)
    },
    test("decode valid uncompressed secp256k1 public key successfully") {
      // priv: 0xe005dfce415d0ff46485fa37a0f035cf02fedf4b611248eb851a6b563dcf61ed
      // pub: 0x0436e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd687c6ced9dbaaa3aca617223c5adaab1109dea0a9a2a75b8fb16361cd19c05f670
      // x: 24826623847575292847053498302447868806247480920702231255375760740584438349160
      // y: 56279252673575028438963532839788487911234756883910418647241582529468321625712
      val uncompressed =
        "0436e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd687c6ced9dbaaa3aca617223c5adaab1109dea0a9a2a75b8fb16361cd19c05f670"
      val bytes = HexString.fromString(uncompressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        _ = apollo.ecKeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
      } yield assertCompletes
    },
    test("decode valid compressed secp256k1 public key successfully (even)") {
      // priv: 0xe005dfce415d0ff46485fa37a0f035cf02fedf4b611248eb851a6b563dcf61ed
      // pub: 0x0236e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd68
      // x: 24826623847575292847053498302447868806247480920702231255375760740584438349160
      // y: 56279252673575028438963532839788487911234756883910418647241582529468321625712
      val compressed = "0236e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd68"
      val bytes = HexString.fromString(compressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        _ = apollo.ecKeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
      } yield assertCompletes
    },
    test("decode valid compressed secp256k1 public key successfully (odd)") {
      // priv: 0xf7ce236f71334ec2e72c31a7b32d9cbbe32d6ff6dd8fe44cfe54188863898143
      // pub: 0x0355b70ea67cf7341b68c83b67058651478d32be4654f5d31e06c1269529e4f68c
      // x: 38770026255392506965090502006036652798042732817741466608725886725558467098252
      // y: 62085974367854462711068547354621142989016502212980790785921388752460910718337
      val compressed = "0355b70ea67cf7341b68c83b67058651478d32be4654f5d31e06c1269529e4f68c"
      val bytes = HexString.fromString(compressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        _ = apollo.ecKeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
      } yield assertCompletes
    },
    test("decode public key yield same result as giving EC point") {
      // priv: 0x2789649b57d8f5df144a817f660b494e7a86d465ba86a638a2b525884c5c5849
      // pub: 0x037b7e17f0524db221af0dd74bd21dec2fc6d0955bbfd43ec7d96ca61dbee2d9d1
      // x: 55857268325124588620525700020439091507381445732605907422424441486941792426449
      // y: 36684214325164537089180371592352190153822062261502257266280631050350493669941
      val compressed = "037b7e17f0524db221af0dd74bd21dec2fc6d0955bbfd43ec7d96ca61dbee2d9d1"
      val bytes = HexString.fromString(compressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        pk1 = apollo.ecKeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
        pk2 = apollo.ecKeyFactory
          .publicKeyFromCoordinate(
            EllipticCurve.SECP256K1,
            BigInt("55857268325124588620525700020439091507381445732605907422424441486941792426449"),
            BigInt("36684214325164537089180371592352190153822062261502257266280631050350493669941")
          )
          .get
      } yield assert(pk1)(equalTo(pk2))
    },
    test("decode compressed and uncompressed of the same key") {
      // priv: 0xe005dfce415d0ff46485fa37a0f035cf02fedf4b611248eb851a6b563dcf61ed
      // pub: 0x0236e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd68
      // pub: 0x0436e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd687c6ced9dbaaa3aca617223c5adaab1109dea0a9a2a75b8fb16361cd19c05f670
      // x: 24826623847575292847053498302447868806247480920702231255375760740584438349160
      // y: 56279252673575028438963532839788487911234756883910418647241582529468321625712
      val compressed = "0236e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd68"
      val uncompressed =
        "0436e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd687c6ced9dbaaa3aca617223c5adaab1109dea0a9a2a75b8fb16361cd19c05f670"
      val bytes = HexString.fromString(compressed).get.toByteArray
      val bytes2 = HexString.fromString(uncompressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        pk1 = apollo.ecKeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
        pk2 = apollo.ecKeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes2).get
      } yield assert(pk1)(equalTo(pk2))
    },
  )

}
