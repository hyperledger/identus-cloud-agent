package io.iohk.atala.mercury.resolvers

import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import io.iohk.atala.resolvers.PeerDidMediatorDidDoc
import org.didcommx.didcomm.common.{VerificationMaterial, VerificationMaterialFormat, VerificationMethodType}
import org.didcommx.didcomm.secret.{Secret, SecretResolverInMemory}
import org.didcommx.peerdid.*
import org.didcommx.peerdid.core.PeerDIDUtils

import scala.jdk.CollectionConverters.*

object PeerDidMediatorSecretResolver {

  val keyIdAgreement = PeerDidMediatorDidDoc.keyIdAgreement
  val keyIdAuthentication = PeerDidMediatorDidDoc.keyAuthentication
  val peerDidMediator = PeerDidMediatorDidDoc.peerDidMediator

  private val secretKeyAgreement = new Secret(
    s"$peerDidMediator#$keyIdAgreement",
    VerificationMethodType.JSON_WEB_KEY_2020,
    new VerificationMaterial(VerificationMaterialFormat.JWK, PeerDidMediatorDidDoc.jwkKeyX25519.toJSONString)
  )
  private val secretKeyAuthentication = new Secret(
    s"$peerDidMediator#$keyIdAuthentication",
    VerificationMethodType.JSON_WEB_KEY_2020,
    new VerificationMaterial(VerificationMaterialFormat.JWK, PeerDidMediatorDidDoc.jwkKeyEd25519.toJSONString)
  )

  val secretResolver = new SecretResolverInMemory(
    Map(
      s"$peerDidMediator#$keyIdAgreement" -> secretKeyAgreement,
      s"$peerDidMediator#$keyIdAuthentication" -> secretKeyAuthentication,
    ).asJava
  )

}
