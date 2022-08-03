package io.iohk.atala.resolvers

import org.didcommx.didcomm.common.{VerificationMaterial, VerificationMaterialFormat, VerificationMethodType}
import org.didcommx.didcomm.secret.{Secret, SecretResolverInMemory}

import scala.jdk.CollectionConverters._

object MediatorSecretResolver {
  val secretKey1 = new Secret(
    "did:example:mediator#key-3",
    VerificationMethodType.JSON_WEB_KEY_2020,
    new VerificationMaterial(
      VerificationMaterialFormat.JWK,
      """{
        |                           "kty":"EC",
        |                           "d":"N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA",
        |                           "crv":"secp256k1",
        |                           "x":"aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
        |                           "y":"JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
        |}""".stripMargin
    )
  )
  val secretKeyAgreement1 = new Secret(
    "did:example:mediator#key-agreement-1",
    VerificationMethodType.JSON_WEB_KEY_2020,
    new VerificationMaterial(
      VerificationMaterialFormat.JWK,
      """{
        |                           "kty":"OKP",
        |                           "d":"b9NnuOCB0hm7YGNvaE9DMhwH_wjZA1-gWD6dA0JWdL0",
        |                           "crv":"X25519",
        |                           "x":"GDTrI66K0pFfO54tlCSvfjjNapIs44dzpneBgyx0S3E"
        |}""".stripMargin
    )
  )

  val secretKeyAgreement2 = new Secret(
    "did:example:mediator#key-agreement-1",
    VerificationMethodType.JSON_WEB_KEY_2020,
    new VerificationMaterial(
      VerificationMaterialFormat.JWK,
      """{
        |                           "kty":"OKP",
        |                           "d":"p-vteoF1gopny1HXywt76xz_uC83UUmrgszsI-ThBKk",
        |                           "crv":"X25519",
        |                           "x":"UT9S3F5ep16KSNBBShU2wh3qSfqYjlasZimn0mB8_VM"
        |}""".stripMargin
    )
  )

  val secretResolver = new SecretResolverInMemory(
    Map(
      "did:example:mediator#key-3" -> secretKey1,
      "did:example:mediator#key-agreement-1" -> secretKeyAgreement1,
      "did:example:mediator#key-agreement-2" -> secretKeyAgreement2
    ).asJava
  )
}
