package io.iohk.atala.mercury

import org.didcommx.didcomm.common.{VerificationMaterial, VerificationMaterialFormat, VerificationMethodType}
import org.didcommx.didcomm.secret.{Secret, SecretResolverInMemory}
import org.didcommx.peerdid.core.PeerDIDUtils
import org.didcommx.peerdid.*

import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import io.circe._
import io.circe.generic.semiauto._
import cats.implicits._
import io.circe.syntax._
import io.circe.generic.semiauto._
import scala.jdk.CollectionConverters.*

import io.iohk.atala.mercury.model.DidId

final case class PeerDID(
    did: DidId,
    keyAgreement: VerificationMaterialPeerDID[VerificationMethodTypeAgreement],
    jwkForKeyAgreement: OctetKeyPair,
    keyAuthentication: VerificationMaterialPeerDID[VerificationMethodTypeAuthentication],
    jwkForKeyAuthentication: OctetKeyPair,
) {
  def getSecretResolverInMemory: SecretResolverInMemory = {
    val keyIdAgreement = PeerDIDUtils.createMultibaseEncnumbasis(keyAgreement).drop(1)
    val keyIdAuthentication = PeerDIDUtils.createMultibaseEncnumbasis(keyAuthentication).drop(1)

    val secretKeyAgreement = new Secret(
      s"${did.value}#$keyIdAgreement",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(VerificationMaterialFormat.JWK, jwkForKeyAgreement.toJSONString)
    )
    val secretKeyAuthentication = new Secret(
      s"${did.value}#$keyIdAuthentication",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(VerificationMaterialFormat.JWK, jwkForKeyAuthentication.toJSONString)
    )

    new SecretResolverInMemory(
      Map(
        s"${did.value}#$keyIdAgreement" -> secretKeyAgreement,
        s"${did.value}#$keyIdAuthentication" -> secretKeyAuthentication,
      ).asJava
    )
  }

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

  def makeNewJwkKeyX25519: OctetKeyPair = new OctetKeyPairGenerator(Curve.X25519).generate()

  def makeNewJwkKeyEd25519: OctetKeyPair = new OctetKeyPairGenerator(Curve.Ed25519).generate()

  def makePeerDid(
      jwkForKeyAgreement: OctetKeyPair = makeNewJwkKeyX25519,
      jwkForKeyAuthentication: OctetKeyPair = makeNewJwkKeyEd25519,
      serviceEndpoint: Option[String] = None
  ): PeerDID = {

    val keyAgreement = VerificationMaterialPeerDID[VerificationMethodTypeAgreement](
      VerificationMaterialFormatPeerDID.JWK,
      jwkForKeyAgreement.toPublicJWK,
      VerificationMethodTypeAgreement.JSON_WEB_KEY_2020.INSTANCE
    )

    def keyAuthentication = VerificationMaterialPeerDID[VerificationMethodTypeAuthentication](
      VerificationMaterialFormatPeerDID.JWK,
      jwkForKeyAuthentication.toPublicJWK,
      VerificationMethodTypeAuthentication.JSON_WEB_KEY_2020.INSTANCE
    )

    val did = org.didcommx.peerdid.PeerDIDCreator.createPeerDIDNumalgo2(
      List(keyAgreement).asJava,
      List(keyAuthentication).asJava,
      serviceEndpoint match {
        case Some(endpoint) => Service(endpoint).asJson.noSpaces
        case None           => null
      }
    )

    PeerDID(
      DidId(did),
      keyAgreement,
      jwkForKeyAgreement,
      keyAuthentication,
      jwkForKeyAuthentication,
    )
  }
}
