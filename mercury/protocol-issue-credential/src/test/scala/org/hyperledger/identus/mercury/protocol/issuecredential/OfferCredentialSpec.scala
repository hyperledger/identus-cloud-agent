package org.hyperledger.identus.mercury.protocol.issuecredential

import io.circe.parser.*
import io.circe.syntax.*
import io.circe.Json
import munit.*
import org.hyperledger.identus.mercury.model.{AttachmentDescriptor, DidId}
import org.hyperledger.identus.mercury.model.AttachmentDescriptor.attachmentDescriptorEncoderV2

class OfferCredentialSpec extends ZSuite {

  test("Issuer OfferCredential") {

    val attribute1 = Attribute(name = "name", value = "Joe Blog")
    val attribute2 = Attribute(name = "dob", value = "01/10/1947")
    val credentialPreview = CredentialPreview(attributes = Seq(attribute1, attribute2))
    val body = OfferCredential.Body(goal_code = Some("Offer Credential"), credential_preview = credentialPreview)
    val attachmentDescriptor = AttachmentDescriptor.buildJsonAttachment[CredentialPreview](payload = credentialPreview)
    val attachmentDescriptorJson = attachmentDescriptor.asJson.deepDropNullValues.noSpaces

    val expectedOfferCredentialJson = parse(s"""{
         |    "id": "041bf917-2cbe-460b-8d12-b1a9609505c2",
         |    "type": "https://didcomm.org/issue-credential/3.0/offer-credential",
         |    "body": {
         |      "goal_code": "Offer Credential",
         |      "credential_preview": {
         |        "type": "https://didcomm.org/issue-credential/3.0/credential-credential",
         |        "body": {
         |          "attributes": [
         |            { "name": "name", "value": "Joe Blog"},
         |            { "name": "dob", "value": "01/10/1947"}
         |          ]
         |        }
         |      }
         |    },
         |    "attachments": [
         |    $attachmentDescriptorJson
         |    ],
         |    "to" : "did:prism:test123",
         |    "from" : "did:prism:test123"
         |}""".stripMargin).getOrElse(Json.Null)

    val offerCredential = OfferCredential(
      id = "041bf917-2cbe-460b-8d12-b1a9609505c2",
      body = body,
      attachments = Seq(attachmentDescriptor),
      to = Some(DidId("did:prism:test123")),
      from = DidId("did:prism:test123")
    )

    val result = offerCredential.asJson.deepDropNullValues

    assertEquals(result, expectedOfferCredentialJson)
  }
}
