package io.iohk.atala.mercury

import zio._
import io.circe._
import io.circe.syntax._

import io.iohk.atala.mercury.model.{_, given}
import io.iohk.atala.mercury.protocol.issuecredential._
import java.io.IOException

object AgentHardCode extends ZIOAppDefault {

  def run = for {
    agentDID <- for {
      peer <- ZIO.succeed(PeerDID.makePeerDid()) // (serviceEndpoint = serviceEndpoint))
      _ <- Console.printLine(s"New DID: ${peer.did}") *>
        Console.printLine(s"JWK for KeyAgreement: ${peer.jwkForKeyAgreement.toJSONString}") *>
        Console.printLine(s"JWK for KeyAuthentication: ${peer.jwkForKeyAuthentication.toJSONString}")
    } yield (peer)
    didCommLayer = AgentCli.agentLayer(agentDID)
    _ <- test.provide(didCommLayer)
  } yield ()

  val attribute1 = Attribute(name = "name", value = "Joe Blog")
  val attribute2 = Attribute(name = "dob", value = "01/10/1947")
  val credentialPreview = CredentialPreview(attributes = Seq(attribute1, attribute2))
  val body = ProposeCredential.Body(
    goal_code = Some("Propose Credential"),
    credential_preview = credentialPreview,
    formats = Seq.empty
  )

  def test: ZIO[DidComm, IOException, Unit] = {
    for {
      didCommService <- ZIO.service[DidComm]
      msg = Message(
        `type` = "TEST",
        from = Some(didCommService.myDid),
        to = Seq.empty,
        body = body.asJson.asObject.get
      )
      // signed <- didCommService.packSigned(msg)
      ttt <- didCommService.packEncrypted(msg, to = didCommService.myDid)
      msg2 <- didCommService.unpack(ttt.string)
      _ <- Console.printLine(msg)

      aaa = msg: org.didcommx.didcomm.message.Message
      _ <- Console.printLine("aaaaaaaaaaaaaaaaaaaaaaaaaaaa")
      _ <- Console.printLine(aaa.getAttachments())
    } yield ()
  }

}
