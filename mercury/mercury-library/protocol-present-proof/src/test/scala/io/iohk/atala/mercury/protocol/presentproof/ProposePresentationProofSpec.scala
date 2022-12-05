package io.iohk.atala.mercury.protocol.presentproof

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
class ProposePresentationSpec extends ZSuite {

  test("Prover Propose Presentation") {

    val presentationFormat = PresentationFormat(attach_id = "1", "format1")
    val body = ProposePresentation.Body(goal_code = Some("Propose Presentation"))
    val attachmentDescriptor =
      AttachmentDescriptor("1", Some("application/json"), LinkData(links = Seq("http://test"), hash = "1234"))
    val attachmentDescriptorJson = attachmentDescriptor.asJson.deepDropNullValues.noSpaces

    val expectedProposalJson = parse(s"""{
         |    "id": "061bf917-2cbe-460b-8d12-b1a9609505c2",
         |    "type": "https://didcomm.atalaprism.io/present-proof/3.0/propose-presentation",
         |    "body": {
         |        "goal_code": "Propose Presentation",
         |        "proof_types": []
         |    },
         |    "attachments":[$attachmentDescriptorJson],
         |    "to" : "did:prism:test123",
         |    "from" : "did:prism:test123"
         |}""".stripMargin).getOrElse(Json.Null)

    val proposePresentation = ProposePresentation(
      id = "061bf917-2cbe-460b-8d12-b1a9609505c2",
      body = body,
      attachments = Seq(attachmentDescriptor),
      to = DidId("did:prism:test123"),
      from = DidId("did:prism:test123"),
    )

    val result = proposePresentation.asJson.deepDropNullValues
    assertEquals(result, expectedProposalJson)
  }
}
