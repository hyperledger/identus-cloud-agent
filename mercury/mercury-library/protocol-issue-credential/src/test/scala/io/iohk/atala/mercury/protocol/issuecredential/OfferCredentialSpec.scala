package io.iohk.atala.mercury.protocol.issuecredential

import cats.implicits.*
import io.circe.Json
import io.circe.generic.semiauto.*
import io.circe.parser.*
import io.circe.syntax.*
import io.iohk.atala.mercury.model.{AttachmentDescriptor, DidId}
import io.iohk.atala.mercury.model.AttachmentDescriptor.attachmentDescriptorEncoderV2
import io.iohk.atala.mercury.protocol.issuecredential.*
import munit.*
import zio.*
import io.iohk.atala.mercury.model._
class OfferCredentialSpec extends ZSuite {

  test("Issuer OfferCredential") {

    val attribute1 = Attribute(name = "name", value = "Joe Blog")
    val attribute2 = Attribute(name = "dob", value = "01/10/1947")
    val credentialPreview = CredentialPreview(attributes = Seq(attribute1, attribute2))
    val body = OfferCredential.Body(goal_code = Some("Offer Credential"), credential_preview = credentialPreview)
    val attachmentDescriptor = AttachmentDescriptor.buildAttachment[CredentialPreview](payload = credentialPreview)
    val attachmentDescriptorJson = attachmentDescriptor.asJson.deepDropNullValues.noSpaces

    val expectedOfferCredentialJson = parse(s"""{
         |    "id": "041bf917-2cbe-460b-8d12-b1a9609505c2",
         |    "type": "https://didcomm.org/issue-credential/2.0/offer-credential",
         |    "body":
         |    {
         |        "goal_code": "Offer Credential",
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
         |    "to" : "did:prism:test123"
         |}""".stripMargin).getOrElse(Json.Null)

    val offerCredential = OfferCredential(
      id = "041bf917-2cbe-460b-8d12-b1a9609505c2",
      body = body,
      attachments = Seq(attachmentDescriptor),
      to = DidId("did:prism:test123"),
      from = DidId("did:prism:test123")
    )

    val result = offerCredential.asJson.deepDropNullValues

    println(result.noSpaces)
    assertEquals(result, expectedOfferCredentialJson)
  }
}
