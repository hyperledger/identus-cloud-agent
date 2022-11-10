package io.iohk.atala.mercury.protocol.presentproof

import io.circe.Json
import io.circe.parser.*
import io.circe.syntax.*
import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.mercury.model.AttachmentDescriptor.attachmentDescriptorEncoderV2
import munit.*
import io.iohk.atala.mercury.model._
import zio.*

class PresentationSpec extends ZSuite {

  test("Verifier Presentation") {

    val presentationFormat = PresentationFormat(attach_id = "1", "format1")
    val body = Presentation.Body(goal_code = Some("Presentation"))
    val attachmentDescriptor =
      AttachmentDescriptor("1", Some("application/json"), LinkData(links = Seq("http://test"), hash = "1234"))
    val attachmentDescriptorJson = attachmentDescriptor.asJson.deepDropNullValues.noSpaces

    val expectedProposalJson = parse(s"""{
                         |    "id": "061bf917-2cbe-460b-8d12-b1a9609505c2",
                         |    "type": "https://didcomm.org/present-proof/2.0/presentation",
                         |    "body":
                         |    {
                         |        "goal_code": "Presentation",
                         |         "last_presentation" : true,
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

    val presentation = Presentation(
      id = "061bf917-2cbe-460b-8d12-b1a9609505c2",
      body = body,
      attachments = Seq(attachmentDescriptor),
      to = DidId("did:prism:test123"),
      from = DidId("did:prism:test123")
    )

    val did = DidId("did:prism:test123")
    println("************************")
    println(did.asJson.noSpaces)
    println("************************")

    val result = presentation.asJson.deepDropNullValues
    assertEquals(result, expectedProposalJson)
  }
}
