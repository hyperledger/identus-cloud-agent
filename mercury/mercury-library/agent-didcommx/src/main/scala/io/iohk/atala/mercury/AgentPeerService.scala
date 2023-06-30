package io.iohk.atala.mercury

import zio._

import io.iohk.atala.mercury.model.*
import com.nimbusds.jose.jwk.OctetKeyPair
import org.didcommx.peerdid.core.PeerDIDUtils
import org.didcommx.didcomm.common._
import org.didcommx.didcomm.secret._
import scala.jdk.CollectionConverters.*

case class AgentPeerService(
    val id: DidId,
    val jwkForKeyAgreement: Seq[OctetKeyPair],
    val jwkForKeyAuthentication: Seq[OctetKeyPair],
) extends DidAgent

object AgentPeerService {
  def makeLayer(peer: PeerDID): ZLayer[Any, Nothing, DidAgent] = ZLayer.succeed(
    AgentPeerService(
      id = peer.did,
      jwkForKeyAgreement = Seq(peer.jwkForKeyAgreement),
      jwkForKeyAuthentication = Seq(peer.jwkForKeyAuthentication),
    )
  )

  def getSecretResolverInMemory(agent: DidAgent): SecretResolverInMemory = {
    val keyAgreement = PeerDID.keyAgreemenFromPublicJWK(agent.jwkForKeyAgreement.head) // TODO Fix head
    val keyAuthentication = PeerDID.keyAuthenticationFromPublicJWK(agent.jwkForKeyAuthentication.head) // TODO Fix head

    val keyIdAgreement = PeerDIDUtils.createMultibaseEncnumbasis(keyAgreement).drop(1)
    val keyIdAuthentication = PeerDIDUtils.createMultibaseEncnumbasis(keyAuthentication).drop(1)

    val secretKeyAgreement = new Secret(
      s"${agent.id.value}#$keyIdAgreement",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(VerificationMaterialFormat.JWK, agent.jwkForKeyAgreement.head.toJSONString)
    )
    val secretKeyAuthentication = new Secret(
      s"${agent.id.value}#$keyIdAuthentication",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(VerificationMaterialFormat.JWK, agent.jwkForKeyAuthentication.head.toJSONString)
    )

    new SecretResolverInMemory(
      Map(
        s"${agent.id.value}#$keyIdAgreement" -> secretKeyAgreement,
        s"${agent.id.value}#$keyIdAuthentication" -> secretKeyAuthentication,
      ).asJava
    )
  }
}
