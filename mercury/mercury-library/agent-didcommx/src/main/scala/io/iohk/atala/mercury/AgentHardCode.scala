package io.iohk.atala.mercury

import zio.*
import io.circe.*
import io.circe.syntax.*
import io.iohk.atala.mercury.model.{*, given}
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.mercury.protocol.presentproof

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

  val attribute1 = presentproof.Attribute(name = "name", value = "Joe Blog")
  val attribute2 = presentproof.Attribute(name = "dob", value = "01/10/1947")
  val credentialPreview = presentproof.CredentialPreview(attributes = Seq(attribute1, attribute2))
  val body = presentproof.ProposePresentation.Body(
    goal_code = Some("Propose Credential"),
    credential_preview = credentialPreview,
    formats = Seq.empty
  )

  def test: ZIO[DidComm, IOException, Unit] = {
    for {
      didCommService <- ZIO.service[DidComm]
      msg = Message(
        piuri = "TEST",
        from = Some(didCommService.myDid),
        to = None,
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
