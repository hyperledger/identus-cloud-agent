package io.iohk.atala.pollux.core.service

import zio.*
import zio.test.*

import io.iohk.atala.pollux.core.repository.CredentialRepositoryInMemory
import io.iohk.atala.mercury.model.DidId
import java.util.UUID
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.grpc.ManagedChannelBuilder
import cats.syntax.validated
import io.iohk.atala.mercury.protocol.issuecredential.OfferCredential
import io.iohk.atala.mercury.protocol.issuecredential.Attribute

object CredentialServiceImplSpec extends ZIOSpecDefault {

  val irisStubLayer = ZLayer.fromZIO(
    ZIO.succeed(IrisServiceGrpc.stub(ManagedChannelBuilder.forAddress("localhost", 9999).usePlaintext.build))
  )
  val credentialServiceLayer = irisStubLayer ++ CredentialRepositoryInMemory.layer >>> CredentialServiceImpl.layer

  override def spec = {
    suite("CredentialServiceImpl")(
      test("createIssuerCredentialRecord creates a valid issuer credential record") {
        check(
          Gen.uuid,
          Gen.option(Gen.string),
          Gen.option(Gen.double),
          Gen.option(Gen.boolean),
          Gen.option(Gen.boolean)
        ) { (thid, schemaId, validityPeriod, automaticIssuance, awaitConfirmation) =>
          for {
            svc <- ZIO.service[CredentialService]
            did = DidId("did:prism:INVITER")
            subjectId = "did:prism:HOLDER"
            record <- svc.createIssueCredentialRecord(
              pairwiseDID = did,
              thid = thid,
              subjectId = subjectId,
              schemaId = schemaId,
              claims = Map("firstname" -> "Alice"),
              validityPeriod = validityPeriod,
              automaticIssuance = automaticIssuance,
              awaitConfirmation = awaitConfirmation
            )
          } yield {
            assertTrue(record.thid == thid) &&
            assertTrue(record.schemaId == schemaId) &&
            assertTrue(record.validityPeriod == validityPeriod) &&
            assertTrue(record.automaticIssuance == automaticIssuance) &&
            assertTrue(record.awaitConfirmation == awaitConfirmation) &&
            assertTrue(record.offerCredentialData.isDefined) &&
            assertTrue(record.offerCredentialData.get.from == did) &&
            assertTrue(record.offerCredentialData.get.to == DidId(subjectId)) &&
            assertTrue(record.offerCredentialData.get.attachments.isEmpty) &&
            assertTrue(record.offerCredentialData.get.thid == Some(thid.toString)) &&
            assertTrue(record.offerCredentialData.get.body.comment == None) &&
            assertTrue(record.offerCredentialData.get.body.goal_code == Some("Offer Credential")) &&
            assertTrue(record.offerCredentialData.get.body.multiple_available == None) &&
            assertTrue(record.offerCredentialData.get.body.replacement_id == None) &&
            assertTrue(record.offerCredentialData.get.body.formats.isEmpty) &&
            assertTrue(
              record.offerCredentialData.get.body.credential_preview.attributes == Seq(
                Attribute("firstname", "Alice", None)
              )
            )
          }
        }
      }
    ).provideLayer(credentialServiceLayer)
  }

}
