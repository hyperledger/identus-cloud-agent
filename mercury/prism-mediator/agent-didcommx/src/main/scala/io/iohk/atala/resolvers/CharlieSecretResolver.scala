package io.iohk.atala.resolvers

import org.didcommx.didcomm.common.{VerificationMaterial, VerificationMaterialFormat, VerificationMethodType}
import org.didcommx.didcomm.secret.{Secret, SecretResolverInMemory}
import scala.jdk.CollectionConverters._
import org.didcommx.peerdid._

object CharlieSecretResolver {

  val jwkKey1 =
    """{
      |  "kty":"EC",
      |  "d":"N3Hm1LXA210YVGGsXw_GklMwcLu_bMgnzDese6YQIyA",
      |  "crv":"secp256k1",
      |  "x":"aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
      |  "y":"JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
      |}""".stripMargin

  val jwkKey2 = // example from did:example:alice#key-1
    """  {
    "kid":"did:example:charlie#key-agreement-1",
    "kty":"OKP",
    "d":"pFRUKkyzx4kHdJtFSnlPA9WzqkDT1HWV0xZ5OYZd2SY",
    "crv":"Ed25519",
    "x":"G-boxFB6vOZBu-wXkm-9Lh79I8nf9Z50cILaOgKKGww"
  },""".stripMargin

  val secretKey1 = new Secret(
    "did:example:charlie#key-3",
    VerificationMethodType.JSON_WEB_KEY_2020,
    new VerificationMaterial(VerificationMaterialFormat.JWK, jwkKey1)
  )
  val secretKeyAgreement1 = new Secret(
    "did:example:charlie#key-agreement-1",
    VerificationMethodType.JSON_WEB_KEY_2020,
    new VerificationMaterial(VerificationMaterialFormat.JWK, jwkKey2)
  )

  val secretResolver = new SecretResolverInMemory(
    Map("did:example:charlie#key-3" -> secretKey1, "did:example:charlie#key-agreement-1" -> secretKeyAgreement1).asJava
  )

  val service =
    """[{
      |  "type": "DIDCommMessaging",
      |  "serviceEndpoint": "http://localhost:8000/",
      |  "routingKeys": ["did:example:somemediator#somekey"]
      |},
      |{
      |  "type": "example",
      |  "serviceEndpoint": "http://localhost:8000/",
      |  "routingKeys": ["did:example:somemediator#somekey2"],
      |  "accept": ["didcomm/v2", "didcomm/aip2;env=rfc587"]
      |}]""".stripMargin

  val keyAgreement = VerificationMaterialPeerDID[VerificationMethodTypeAgreement](
    VerificationMaterialFormatPeerDID.JWK,
    jwkKey1,
    VerificationMethodTypeAgreement.JSON_WEB_KEY_2020.INSTANCE
  )
  val keyAuthentication = VerificationMaterialPeerDID[VerificationMethodTypeAuthentication](
    VerificationMaterialFormatPeerDID.JWK,
    jwkKey2,
    VerificationMethodTypeAuthentication.JSON_WEB_KEY_2020.INSTANCE
  )

  def charlieDID = org.didcommx.peerdid.PeerDIDCreator.createPeerDIDNumalgo2(
    List(keyAgreement).asJava,
    List(keyAuthentication).asJava,
    service
  )

}
