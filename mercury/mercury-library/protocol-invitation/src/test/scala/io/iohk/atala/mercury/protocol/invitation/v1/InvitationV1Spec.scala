package io.iohk.atala.mercury.protocol.invitation.v1

import munit.*
import zio.*
import cats.implicits._
import io.circe.syntax._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._

import io.iohk.atala.mercury.protocol.invitation._
import io.iohk.atala.mercury.protocol.invitation.InvitationCodec._
import io.iohk.atala.mercury.model.AttachmentDescriptor

class InvitationV1Spec extends ZSuite {

  test("out-of-band invitation") {
    val payload = "attachmentData"
    val payloadBase64 = java.util.Base64.getUrlEncoder.encodeToString(Json.fromString(payload).noSpaces.getBytes)

    val expectedJson = parse(s"""{
                         |  "@id": "f3375429-b116-4224-b55f-563d7ef461f1",
                         |  "@type": "https://didcomm.org/out-of-band/2.0/invitation",
                         |  "label": "Faber College",
                         |  "goal": "To issue a Faber College Graduate credential",
                         |  "goal_code": "issue-vc",
                         |  "accept": [
                         |    "didcomm/aip2;env=rfc587",
                         |    "didcomm/aip2;env=rfc19"
                         |  ],
                         |  "handshake_protocols": [
                         |    "https://didcomm.org/didexchange/1.0",
                         |    "https://didcomm.org/connections/1.0"
                         |  ],
                         |  "requests~attach": [
                         |    {
                         |      "@id": "request-0",
                         |      "mime-type": "application/json",
                         |      "data": {"base64" : "$payloadBase64"}
                         |    }
                         |  ],
                         |  "services": ["did:sov:LjgpST2rjsoxYegQDRm7EL"]
                         |}""".stripMargin).getOrElse(Json.Null)

    val service = Service(
      id = "did:prism:PR6vs6GEZ8rHaVgjg2WodM#did-communication",
      `type` = "did-communication",
      recipientKeys = Seq("did:prism:PR6vs6GEZ8rHaVgjg2WodM"),
      routingKeys = Some(Seq("did:prism:PR6vs6GEZ8rHaVgjg2WodM")),
      serviceEndpoint = "http://localhost:8080/service"
    )
    val did = Did("did:sov:LjgpST2rjsoxYegQDRm7EL")
    val accepts = Seq("didcomm/aip2;env=rfc587", "didcomm/aip2;env=rfc19")
    val handshakeProtocols = Seq("https://didcomm.org/didexchange/1.0", "https://didcomm.org/connections/1.0")

    val attachmentDescriptor = AttachmentDescriptor.buildAttachment(id = Some("request-0"), payload = payload)

    val invitation = Invitation(
      `@id` = "f3375429-b116-4224-b55f-563d7ef461f1",
      label = "Faber College",
      goal = "To issue a Faber College Graduate credential",
      goal_code = "issue-vc",
      accept = accepts,
      handshake_protocols = handshakeProtocols,
      `requests~attach` = Seq(attachmentDescriptor),
      services = Seq(did)
    )
    val result = invitation.asJson.deepDropNullValues
    assertEquals(result, expectedJson)
  }
}
