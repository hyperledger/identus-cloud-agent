package io.iohk.atala.mercury

import zio._
import io.circe._
import io.circe.syntax._

import io.iohk.atala.mercury.model.{given, _}
import io.iohk.atala.mercury.protocol.issuecredential._
import java.io.IOException

import scala.language.implicitConversions

object AgentHardCode extends ZIOAppDefault {

  def run = for {
    didPeer <- for {
      peer <- ZIO.succeed(PeerDID.makePeerDid()) // (serviceEndpoint = serviceEndpoint))
      _ <- Console.printLine(s"New DID: ${peer.did}") *>
        Console.printLine(s"JWK for KeyAgreement: ${peer.jwkForKeyAgreement.toJSONString}") *>
        Console.printLine(s"JWK for KeyAuthentication: ${peer.jwkForKeyAuthentication.toJSONString}")
    } yield (peer)
    _ <- test.provide(DidCommX.liveLayer, AgentPeerService.makeLayer(didPeer))
  } yield ()

  val attribute1 = Attribute(name = "name", value = "Joe Blog")
  val attribute2 = Attribute(name = "dob", value = "01/10/1947")
  val credentialPreview = CredentialPreview(attributes = Seq(attribute1, attribute2))
  val body = ProposeCredential.Body(
    goal_code = Some("Propose Credential"),
    credential_preview = credentialPreview,
    formats = Seq.empty
  )

  def test: ZIO[DidOps & DidAgent, IOException, Unit] = {
    for {
      agentService <- ZIO.service[DidAgent]
      opsService <- ZIO.service[DidOps]
      msg = Message(
        `type` = "TEST",
        from = Some(agentService.id),
        to = Seq.empty,
        body = body.asJson.asObject.get
      )
      // signed <- didCommService.packSigned(msg)
      ttt <- opsService.packEncrypted(msg, to = agentService.id)
      msg2 <- opsService.unpack(ttt.string)
      _ <- Console.printLine(msg)

      aaa = msg: org.didcommx.didcomm.message.Message
      _ <- Console.printLine("aaaaaaaaaaaaaaaaaaaaaaaaaaaa")
      _ <- Console.printLine(aaa.getAttachments())
    } yield ()
  }

}
