package io.iohk.atala.mercury.protocol.presentproof

import cats.implicits.*
import io.circe.Json
import io.circe.generic.semiauto.*
import io.circe.parser.*
import io.circe.syntax.*
import io.iohk.atala.mercury.model.AttachmentDescriptor.attachmentDescriptorEncoderV2
import io.iohk.atala.mercury.model.{AttachmentDescriptor, DidId}
import munit.*
import zio.*
import io.iohk.atala.mercury.model._
class RequestCredentialSpec extends ZSuite {

  test("Verifier Request Presentation") {

    val presentationFormat = PresentationFormat(attach_id = "1", "format1")
    val body = RequestPresentation.Body(goal_code = Some("Propose Presentation"))
    val attachmentDescriptor =
      AttachmentDescriptor("1", Some("application/json"), LinkData(links = Seq("http://test"), hash = "1234"))
    val attachmentDescriptorJson = attachmentDescriptor.asJson.deepDropNullValues.noSpaces

    val expectedProposalJson = parse(s"""{
         |    "id": "061bf917-2cbe-460b-8d12-b1a9609505c2",
         |    "type": "https://didcomm.atalaprism.io/present-proof/3.0/request-presentation",
         |    "body":
         |    {
         |        "goal_code": "Propose Presentation",
         |        "will_confirm" : false,
         |        "present_multiple" : false,
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

    val requestPresentation = RequestPresentation(
      id = "061bf917-2cbe-460b-8d12-b1a9609505c2",
      body = body,
      attachments = Seq(attachmentDescriptor),
      to = DidId("did:prism:test123"),
      from = DidId("did:prism:test123"),
    )

    val result = requestPresentation.asJson.deepDropNullValues
    assertEquals(result, expectedProposalJson)
  }
}
