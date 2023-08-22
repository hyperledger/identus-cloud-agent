package io.iohk.atala.mercury

import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.iohk.atala.mercury.model.DidId
import org.didcommx.peerdid.*

import scala.jdk.CollectionConverters.*

final case class PeerDID(
    did: DidId,
    jwkForKeyAgreement: JWK,
    jwkForKeyAuthentication: JWK,
) {
  // def keyAgreement = PeerDID.keyAgreemenFromPublicJWK(jwkForKeyAgreement)
  // def keyAuthentication = PeerDID.keyAuthenticationFromPublicJWK(jwkForKeyAuthentication)

  // def getSecretResolverInMemory: SecretResolverInMemory = {
  //   val keyIdAgreement = PeerDIDUtils.createMultibaseEncnumbasis(keyAgreement).drop(1)
  //   val keyIdAuthentication = PeerDIDUtils.createMultibaseEncnumbasis(keyAuthentication).drop(1)

  //   val secretKeyAgreement = new Secret(
  //     s"${did.value}#$keyIdAgreement",
  //     VerificationMethodType.JSON_WEB_KEY_2020,
  //     new VerificationMaterial(VerificationMaterialFormat.JWK, jwkForKeyAgreement.toJSONString)
  //   )
  //   val secretKeyAuthentication = new Secret(
  //     s"${did.value}#$keyIdAuthentication",
  //     VerificationMethodType.JSON_WEB_KEY_2020,
  //     new VerificationMaterial(VerificationMaterialFormat.JWK, jwkForKeyAuthentication.toJSONString)
  //   )

  //   new SecretResolverInMemory(
  //     Map(
  //       s"${did.value}#$keyIdAgreement" -> secretKeyAgreement,
  //       s"${did.value}#$keyIdAuthentication" -> secretKeyAuthentication,
  //     ).asJava
  //   )
  // }

  def getDIDDocument = org.didcommx.peerdid.PeerDIDResolver
    .resolvePeerDID(did.value, VerificationMaterialFormatPeerDID.JWK)
}

object PeerDID {

  /** PeerDidServiceEndpoint
    *
    * @param r
    *   routingKeys are OPTIONAL. An ordered array of strings referencing keys to be used when preparing the message for
    *   transmission as specified in Sender Process to Enable Forwarding, above.
    */
  case class Service(
      t: String = "dm",
      s: String,
      r: Seq[String] = Seq.empty,
      a: Seq[String] = Seq("didcomm/v2")
  ) {
    def `type` = t
    def serviceEndpoint = s
    def routingKeys = r
    def accept = a
  }
  object Service {
    implicit val encoder: Encoder[Service] = deriveEncoder[Service]
    implicit val decoder: Decoder[Service] = deriveDecoder[Service]
    def apply(endpoint: String) = new Service(s = endpoint)
  }

  def makeNewJwkKeyX25519: JWK = new OctetKeyPairGenerator(Curve.X25519).generate()

  def makeNewJwkKeyEd25519: JWK = new OctetKeyPairGenerator(Curve.Ed25519).generate()

  def keyAgreemenFromPublicJWK(key: JWK) = VerificationMaterialPeerDID[VerificationMethodTypeAgreement](
    VerificationMaterialFormatPeerDID.JWK,
    key.toPublicJWK,
    VerificationMethodTypeAgreement.JSON_WEB_KEY_2020.INSTANCE
  )

  def keyAuthenticationFromPublicJWK(key: JWK) =
    VerificationMaterialPeerDID[VerificationMethodTypeAuthentication](
      VerificationMaterialFormatPeerDID.JWK,
      key.toPublicJWK,
      VerificationMethodTypeAuthentication.JSON_WEB_KEY_2020.INSTANCE
    )

  def makePeerDid(
      jwkForKeyAgreement: JWK = makeNewJwkKeyX25519,
      jwkForKeyAuthentication: JWK = makeNewJwkKeyEd25519,
      serviceEndpoint: Option[String] = None
  ): PeerDID = {
    val did = org.didcommx.peerdid.PeerDIDCreator.createPeerDIDNumalgo2(
      List(keyAgreemenFromPublicJWK(jwkForKeyAgreement)).asJava,
      List(keyAuthenticationFromPublicJWK(jwkForKeyAuthentication)).asJava,
      serviceEndpoint match {
        case Some(endpoint) => Service(endpoint).asJson.noSpaces
        case None           => null
      }
    )
    PeerDID(DidId(did), jwkForKeyAgreement, jwkForKeyAuthentication)
  }
}
