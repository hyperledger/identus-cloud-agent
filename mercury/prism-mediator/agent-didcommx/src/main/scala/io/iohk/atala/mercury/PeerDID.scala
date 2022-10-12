package io.iohk.atala.mercury

import org.didcommx.didcomm.common.{VerificationMaterial, VerificationMaterialFormat, VerificationMethodType}
import org.didcommx.didcomm.secret.{Secret, SecretResolverInMemory}
import org.didcommx.peerdid.core.PeerDIDUtils
import org.didcommx.peerdid.*

import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*

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

  def makeNewJwkKeyX25519: OctetKeyPair = new OctetKeyPairGenerator(Curve.X25519).generate()

  def makeNewJwkKeyEd25519: OctetKeyPair = new OctetKeyPairGenerator(Curve.Ed25519).generate()

  def makePeerDid(
      jwkForKeyAgreement: OctetKeyPair = makeNewJwkKeyX25519,
      jwkForKeyAuthentication: OctetKeyPair = makeNewJwkKeyEd25519
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
      null, // service
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
