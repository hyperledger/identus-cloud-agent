package io.iohk.atala.mercury

import org.didcommx.didcomm.DIDComm

import zio._
import org.didcommx.didcomm.model._

import io.iohk.atala.resolvers._
import io.iohk.atala.mercury.model.{given, _}
import java.util.Base64
import scala.annotation.nowarn
import com.nimbusds.jose.jwk.OctetKeyPair
import org.didcommx.didcomm.diddoc.DIDDoc
import org.didcommx.peerdid.core.PeerDIDUtils
import org.didcommx.didcomm.common._
import org.didcommx.didcomm.secret._
import scala.jdk.CollectionConverters.*

case class AgentPeerService(
    val id: DidId,
    val jwkForKeyAgreement: Seq[OctetKeyPair],
    val jwkForKeyAuthentication: Seq[OctetKeyPair],
) extends DidAgent {

  def keyAgreement = PeerDID.keyAgreemenFromPublicJWK(jwkForKeyAgreement.head) // TODO Fix head
  def keyAuthentication = PeerDID.keyAuthenticationFromPublicJWK(jwkForKeyAuthentication.head) // TODO Fix head

  def getSecretResolverInMemory: SecretResolverInMemory = {
    val keyIdAgreement = PeerDIDUtils.createMultibaseEncnumbasis(keyAgreement).drop(1)
    val keyIdAuthentication = PeerDIDUtils.createMultibaseEncnumbasis(keyAuthentication).drop(1)

    val secretKeyAgreement = new Secret(
      s"${id.value}#$keyIdAgreement",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(VerificationMaterialFormat.JWK, jwkForKeyAgreement.head.toJSONString)
    )
    val secretKeyAuthentication = new Secret(
      s"${id.value}#$keyIdAuthentication",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(VerificationMaterialFormat.JWK, jwkForKeyAuthentication.head.toJSONString)
    )

    new SecretResolverInMemory(
      Map(
        s"${id.value}#$keyIdAgreement" -> secretKeyAgreement,
        s"${id.value}#$keyIdAuthentication" -> secretKeyAuthentication,
      ).asJava
    )
  }
}

object AgentPeerService {
  def makeLayer(peer: PeerDID): ZLayer[Any, Nothing, DidAgent] = ZLayer.succeed(
    AgentPeerService(
      did = peer.did,
      jwkForKeyAgreement = Seq(peer.jwkForKeyAgreement),
      jwkForKeyAuthentication = Seq(peer.jwkForKeyAuthentication),
    )
  )
}

object DidCommX {
  def makeLayer(peer: PeerDID): ZLayer[Any, Nothing, DidOps & DidAgent] = ZLayer.succeed(
    DidCommX(new DIDComm(UniversalDidResolver, peer.getSecretResolverInMemory), peer.did)
  )
}
class DidCommX(didComm: DIDComm, fixme: DidId) extends DidOps with DidAgent /* with DIDResolver */ {

  override def id: DidId = fixme // FIXME the Secret is on org.didcommx.didcomm.model.DIDComm ...

  // override def resolveDID(did: DidId): Task[DIDDoc] = UniversalDidResolver.resolveDID(did)

  override def packSigned(msg: Message): URIO[DidAgent, SignedMesage] = for {
    agent <- ZIO.service[DidAgent]
    params = new PackSignedParams.Builder(msg, agent.id.value).build()
    ret = didComm.packSigned(params)
  } yield (ret)

  override def packEncrypted(msg: Message, to: DidId): URIO[DidAgent, EncryptedMessage] = for {
    // assert(msg.from == Some(myDid), s"ERROR in packEncrypted: ${msg.from} must be == to ${myDid}") TODO
    agent <- ZIO.service[DidAgent]
    params = new PackEncryptedParams.Builder(msg, to.value)
      .from(agent.id.value)
      .forward(false)
      .build()
    ret = didComm.packEncrypted(params)
  } yield (ret)

  override def packEncryptedAnon(msg: Message, to: DidId): UIO[EncryptedMessage] = {
    val params = new PackEncryptedParams.Builder(msg, to.value)
      // .from(myDid.value)
      .forward(true)
      .build()
    didComm.packEncrypted(params)
    ZIO.succeed(didComm.packEncrypted(params))
  }

  override def unpack(data: String): URIO[DidAgent, UnpackMessage] = {
    ZIO.succeed(didComm.unpack(new UnpackParams.Builder(data).build()))
  }

}

// object AgentService {
//   val alice = ZLayer.succeed(
//     AgentService[Agent.Alice.type](
//       new DIDComm(UniversalDidResolver, AliceSecretResolver.secretResolver),
//       Agent.Alice
//     )
//   )
//   val bob = ZLayer.succeed(
//     AgentService[Agent.Bob.type](
//       new DIDComm(UniversalDidResolver, BobSecretResolver.secretResolver),
//       Agent.Bob
//     )
//   )

//   // val charlie = ZLayer.succeed(
//   //   AgentService[Agent.Charlie.type](
//   //     new DIDComm(UniversalDidResolver, CharlieSecretResolver.secretResolver),
//   //     Agent.Charlie
//   //   )
//   // )

// }
