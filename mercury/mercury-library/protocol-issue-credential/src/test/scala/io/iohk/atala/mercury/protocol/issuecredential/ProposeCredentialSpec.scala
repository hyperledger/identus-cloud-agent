package io.iohk.atala.mercury.protocol.issuecredential

import cats.implicits.*
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.mercury.model.AttachmentDescriptor.attachmentDescriptorEncoderV2
import munit.*
import zio.*
import io.iohk.atala.mercury.model._
class ProposeCredentialSpec extends ZSuite {

  test("Holder ProposeCredential") {

    val attribute1 = Attribute(name = "name", value = "Joe Blog")
    val attribute2 = Attribute(name = "dob", value = "01/10/1947")
    val credentialPreview = CredentialPreview(attributes = Seq(attribute1, attribute2))
    val body = ProposeCredential.Body(goal_code = Some("Propose Credential"), credential_preview = credentialPreview)
    val attachmentDescriptor = AttachmentDescriptor.buildJsonAttachment[CredentialPreview](payload = credentialPreview)
    val attachmentDescriptorJson = attachmentDescriptor.asJson.deepDropNullValues.noSpaces

    val expectedProposalJson = parse(s"""{
                         |    "id": "031bf917-2cbe-460b-8d12-b1a9609505c2",
                         |    "type": "https://didcomm.org/issue-credential/2.0/propose-credential",
                         |    "body":
                         |    {
                         |        "goal_code": "Propose Credential",
                         |        "credential_preview":
                         |        {
                         |            "type": "https://didcomm.org/issue-credential/2.0/credential-preview",
                         |            "attributes":
                         |            [
                         |                {
                         |                    "name": "name",
                         |                    "value": "Joe Blog"
                         |                },
                         |                {
                         |                    "name": "dob",
                         |                    "value": "01/10/1947"
                         |                }
                         |            ]
                         |        },
                         |        "formats":
                         |        []
                         |    },
                         |    "attachments":
                         |    [
                         |    $attachmentDescriptorJson
                         |    ],
                         |    "to" : "did:prism:test123",
                         |    "from" : "did:prism:test123"
                         |}""".stripMargin).getOrElse(Json.Null)

    val proposeCredential = ProposeCredential(
      id = "031bf917-2cbe-460b-8d12-b1a9609505c2",
      body = body,
      attachments = Seq(attachmentDescriptor),
      to = DidId("did:prism:test123"),
      from = DidId("did:prism:test123"),
    )

    val result = proposeCredential.asJson.deepDropNullValues
    assertEquals(result, expectedProposalJson)
  }
}
