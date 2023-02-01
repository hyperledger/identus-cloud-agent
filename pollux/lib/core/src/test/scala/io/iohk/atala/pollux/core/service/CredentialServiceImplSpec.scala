package io.iohk.atala.pollux.core.service

import cats.syntax.validated
import io.circe.parser.decode
import io.circe.syntax._
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.model.Message
import io.iohk.atala.mercury.protocol.issuecredential.Attribute
import io.iohk.atala.mercury.protocol.issuecredential.CredentialPreview
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.mercury.protocol.issuecredential.OfferCredential
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.pollux.core.model.IssueCredentialRecord._
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.model.error.CredentialServiceError._
import io.iohk.atala.pollux.core.repository.CredentialRepositoryInMemory
import zio.*
import zio.test.*

import java.util.UUID

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
            record <- svc.createRecord(
              thid = thid,
              did = did,
              subjectId = subjectId,
              schemaId = schemaId,
              validityPeriod = validityPeriod,
              automaticIssuance = automaticIssuance,
              awaitConfirmation = awaitConfirmation
            )
          } yield {
            assertTrue(record.thid == thid) &&
            assertTrue(record.updatedAt.isEmpty) &&
            assertTrue(record.schemaId == schemaId) &&
            assertTrue(record.subjectId == subjectId) &&
            assertTrue(record.validityPeriod == validityPeriod) &&
            assertTrue(record.automaticIssuance == automaticIssuance) &&
            assertTrue(record.awaitConfirmation == awaitConfirmation) &&
            assertTrue(record.role == Role.Issuer) &&
            assertTrue(record.protocolState == ProtocolState.OfferPending) &&
            assertTrue(record.publicationState.isEmpty) &&
            assertTrue(record.offerCredentialData.isDefined) &&
            assertTrue(record.offerCredentialData.get.from == did) &&
            assertTrue(record.offerCredentialData.get.to == DidId(subjectId)) &&
            assertTrue(record.offerCredentialData.get.attachments.isEmpty) &&
            assertTrue(record.offerCredentialData.get.thid.contains(thid.toString)) &&
            assertTrue(record.offerCredentialData.get.body.comment.isEmpty) &&
            assertTrue(record.offerCredentialData.get.body.goal_code.contains("Offer Credential")) &&
            assertTrue(record.offerCredentialData.get.body.multiple_available.isEmpty) &&
            assertTrue(record.offerCredentialData.get.body.replacement_id.isEmpty) &&
            assertTrue(record.offerCredentialData.get.body.formats.isEmpty) &&
            assertTrue(
              record.offerCredentialData.get.body.credential_preview.attributes == Seq(
                Attribute("name", "Alice", None)
              )
            ) &&
            assertTrue(record.requestCredentialData.isEmpty) &&
            assertTrue(record.issueCredentialData.isEmpty) &&
            assertTrue(record.issuedCredentialRaw.isEmpty)
          }
        }
      },
      test("createIssuerCredentialRecord should reject unsupported `subjectId` format") {
        for {
          svc <- ZIO.service[CredentialService]
          did = DidId("did:prism:INVITER")
          subjectId = "did:unsupported:HOLDER"
          record <- svc.createRecord(did = did, subjectId = subjectId).exit
        } yield {
          assertTrue(record match
            case Exit.Failure(cause: Cause.Fail[_]) if cause.value.isInstanceOf[UnsupportedDidFormat] => true
            case _                                                                                    => false
          )
        }
      },
      test("createIssuerCredentialRecord should reject creation with a duplicate 'thid'") {
        for {
          svc <- ZIO.service[CredentialService]
          thid = UUID.randomUUID()
          aRecord <- svc.createRecord(thid = thid)
          bRecord <- svc.createRecord(thid = thid).exit
        } yield {
          assertTrue(bRecord match
            case Exit.Failure(cause: Cause.Fail[_]) if cause.value.isInstanceOf[RepositoryError] => true
            case _                                                                               => false
          )
        }
      },
      test("getCredentialRecords returns the created records") {
        for {
          svc <- ZIO.service[CredentialService]
          aRecord <- svc.createRecord()
          bRecord <- svc.createRecord()
          records <- svc.getIssueCredentialRecords()
        } yield {
          assertTrue(records.size == 2) &&
          assertTrue(records.contains(aRecord)) &&
          assertTrue(records.contains(bRecord))
        }
      },
      test("getCredentialRecordsByState returns the correct records") {
        for {
          svc <- ZIO.service[CredentialService]
          aRecord <- svc.createRecord()
          records <- svc.getCredentialRecordsByStates(ProtocolState.OfferPending)
          onePending = assertTrue(records.size == 1) && assertTrue(records.contains(aRecord))
          records <- svc.getCredentialRecordsByStates(ProtocolState.OfferSent)
          zeroSent = assertTrue(records.isEmpty)
        } yield onePending && zeroSent
      },
      test("getCredentialRecord returns the correct record") {
        for {
          svc <- ZIO.service[CredentialService]
          aRecord <- svc.createRecord()
          bRecord <- svc.createRecord()
          record <- svc.getIssueCredentialRecord(bRecord.id)
        } yield assertTrue(record.contains(bRecord))
      },
      test("getCredentialRecord returns nothing for an unknown 'recordId'") {
        for {
          svc <- ZIO.service[CredentialService]
          aRecord <- svc.createRecord()
          bRecord <- svc.createRecord()
          record <- svc.getIssueCredentialRecord(UUID.randomUUID())
        } yield assertTrue(record.isEmpty)
      },
      test("receiveCredentialOffer successfully creates a record") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          holderRecord <- holderSvc.receiveCredentialOffer(offer)
        } yield {
          assertTrue(holderRecord.thid.toString == offer.thid.get) &&
          assertTrue(holderRecord.updatedAt.isEmpty) &&
          assertTrue(holderRecord.schemaId.isEmpty) &&
          assertTrue(holderRecord.subjectId == offer.to.value) &&
          assertTrue(holderRecord.validityPeriod.isEmpty) &&
          assertTrue(holderRecord.automaticIssuance.isEmpty) &&
          assertTrue(holderRecord.awaitConfirmation.isEmpty) &&
          assertTrue(holderRecord.role == Role.Holder) &&
          assertTrue(holderRecord.protocolState == ProtocolState.OfferReceived) &&
          assertTrue(holderRecord.publicationState.isEmpty) &&
          assertTrue(holderRecord.offerCredentialData.contains(offer)) &&
          assertTrue(holderRecord.requestCredentialData.isEmpty) &&
          assertTrue(holderRecord.issueCredentialData.isEmpty) &&
          assertTrue(holderRecord.issuedCredentialRaw.isEmpty)
        }
      },
      test("receiveCredentialOffer can't be called twice with the same offer") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          _ <- holderSvc.receiveCredentialOffer(offer)
          exit <- holderSvc.receiveCredentialOffer(offer).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(cause: Cause.Fail[_]) if cause.value.isInstanceOf[RepositoryError] => true
            case _                                                                               => false
          )
        }
      },
      test("acceptCredentialOffer updates the record's protocol state") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(offer)
          offerAcceptedRecord <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id)
        } yield {
          assertTrue(offerAcceptedRecord.isDefined) &&
          assertTrue(offerAcceptedRecord.get.protocolState == ProtocolState.RequestPending) &&
          assertTrue(offerAcceptedRecord.get.offerCredentialData.isDefined) &&
          assertTrue(offerAcceptedRecord.get.offerCredentialData.get.from == offer.from) &&
          assertTrue(offerAcceptedRecord.get.offerCredentialData.get.to == offer.to) &&
          assertTrue(offerAcceptedRecord.get.offerCredentialData.get.attachments.isEmpty) &&
          assertTrue(offerAcceptedRecord.get.offerCredentialData.get.thid == offer.thid) &&
          assertTrue(offerAcceptedRecord.get.offerCredentialData.get.body.comment.isEmpty) &&
          assertTrue(offerAcceptedRecord.get.offerCredentialData.get.body.goal_code.contains("Offer Credential")) &&
          assertTrue(offerAcceptedRecord.get.offerCredentialData.get.body.multiple_available.isEmpty) &&
          assertTrue(offerAcceptedRecord.get.offerCredentialData.get.body.replacement_id.isEmpty) &&
          assertTrue(offerAcceptedRecord.get.offerCredentialData.get.body.formats.isEmpty) &&
          assertTrue(
            offerAcceptedRecord.get.offerCredentialData.get.body.credential_preview.attributes == Seq(
              Attribute("name", "Alice", None)
            )
          )
        }
      },
      test("acceptCredentialOffer cannot be called twice for the same record") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(offer)
          _ <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id)
          exit <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(cause: Cause.Fail[_]) if cause.value.isInstanceOf[InvalidFlowStateError] => true
            case _                                                                                     => false
          )
        }
      },
      test("receiveCredentialRequest successfully updates the record") {
        for {
          issuerSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          issuerRecord <- issuerSvc.createRecord()
          _ <- issuerSvc.markOfferSent(issuerRecord.id)
          request = requestCredential(Some(issuerRecord.thid))
          requestReceivedRecord <- issuerSvc.receiveCredentialRequest(request)
        } yield {
          assertTrue(requestReceivedRecord.isDefined) &&
          assertTrue(requestReceivedRecord.get.protocolState == ProtocolState.RequestReceived) &&
          assertTrue(requestReceivedRecord.get.requestCredentialData.contains(request))
        }
      },
      test("receiveCredentialRequest cannot be called twice for the same record") {
        for {
          issuerSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          issuerRecord <- issuerSvc.createRecord()
          _ <- issuerSvc.markOfferSent(issuerRecord.id)
          request = requestCredential(Some(issuerRecord.thid))
          requestReceivedRecord <- issuerSvc.receiveCredentialRequest(request)
          exit <- issuerSvc.receiveCredentialRequest(request).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(cause: Cause.Fail[_]) if cause.value.isInstanceOf[InvalidFlowStateError] => true
            case _                                                                                     => false
          )
        }
      },
      test("receiveCredentialRequest is rejected for an unknown 'thid'") {
        for {
          issuerSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          issuerRecord <- issuerSvc.createRecord()
          _ <- issuerSvc.markOfferSent(issuerRecord.id)
          request = requestCredential(Some(UUID.randomUUID()))
          exit <- issuerSvc.receiveCredentialRequest(request).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(cause: Cause.Fail[_]) if cause.value.isInstanceOf[ThreadIdNotFound] => true
            case _                                                                                => false
          )
        }
      },
      test("acceptCredentialRequest successfully updates the record") {
        for {
          issuerSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          issuerRecord <- issuerSvc.createRecord()
          _ <- issuerSvc.markOfferSent(issuerRecord.id)
          request = requestCredential(Some(issuerRecord.thid))
          requestReceivedRecord <- issuerSvc.receiveCredentialRequest(request)
          requestAcceptedRecord <- issuerSvc.acceptCredentialRequest(requestReceivedRecord.get.id)
        } yield {
          assertTrue(requestAcceptedRecord.isDefined) &&
          assertTrue(requestAcceptedRecord.get.protocolState == ProtocolState.CredentialPending) &&
          assertTrue(requestAcceptedRecord.get.issueCredentialData.isDefined)
        }
      },
      test("acceptCredentialRequest cannot be called twice for the same record") {
        for {
          issuerSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          issuerRecord <- issuerSvc.createRecord()
          _ <- issuerSvc.markOfferSent(issuerRecord.id)
          request = requestCredential(Some(issuerRecord.thid))
          requestReceivedRecord <- issuerSvc.receiveCredentialRequest(request)
          requestAcceptedRecord <- issuerSvc.acceptCredentialRequest(requestReceivedRecord.get.id)
          exit <- issuerSvc.acceptCredentialRequest(requestReceivedRecord.get.id).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(cause: Cause.Fail[_]) if cause.value.isInstanceOf[InvalidFlowStateError] => true
            case _                                                                                     => false
          )
        }
      },
      test("markCredentialGenerated updates the protocol state and saves the credential") {
        for {
          issuerSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          issuerRecord <- issuerSvc.createRecord()
          _ <- issuerSvc.markOfferSent(issuerRecord.id)
          request = requestCredential(Some(issuerRecord.thid))
          requestReceivedRecord <- issuerSvc.receiveCredentialRequest(request)
          requestAcceptedRecord <- issuerSvc.acceptCredentialRequest(requestReceivedRecord.get.id)
          issue = issueCredential()
          credentialGeneratedRecord <- issuerSvc.markCredentialGenerated(issuerRecord.id, issue)
        } yield {
          assertTrue(credentialGeneratedRecord.isDefined) &&
          assertTrue(credentialGeneratedRecord.get.protocolState == ProtocolState.CredentialGenerated) &&
          assertTrue(credentialGeneratedRecord.get.issueCredentialData.contains(issue))
        }
      },
      test("receiveCredentialIssue successfully updates the record") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(offer)
          _ <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id)
          _ <- holderSvc.markRequestSent(offerReceivedRecord.id)
          issue = issueCredential(thid = Some(offerReceivedRecord.thid))
          credentialReceivedRecord <- holderSvc.receiveCredentialIssue(issue)
        } yield {
          assertTrue(credentialReceivedRecord.isDefined) &&
          assertTrue(credentialReceivedRecord.get.protocolState == ProtocolState.CredentialReceived) &&
          assertTrue(credentialReceivedRecord.get.issueCredentialData.contains(issue))
        }
      },
      test("receiveCredentialIssue cannot be called twice for the same record") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(offer)
          _ <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id)
          _ <- holderSvc.markRequestSent(offerReceivedRecord.id)
          issue = issueCredential(thid = Some(offerReceivedRecord.thid))
          credentialReceivedRecord <- holderSvc.receiveCredentialIssue(issue)
          exit <- holderSvc.receiveCredentialIssue(issue).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(cause: Cause.Fail[_]) if cause.value.isInstanceOf[InvalidFlowStateError] => true
            case _                                                                                     => false
          )
        }
      },
      test("receiveCredentialIssue is rejected for an unknown 'thid'") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(offer)
          _ <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id)
          _ <- holderSvc.markRequestSent(offerReceivedRecord.id)
          issue = issueCredential(thid = Some(UUID.randomUUID()))
          exit <- holderSvc.receiveCredentialIssue(issue).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(cause: Cause.Fail[_]) if cause.value.isInstanceOf[ThreadIdNotFound] => true
            case _                                                                                => false
          )
        }
      },
      test("Happy flow is successfully executed") {
        for {
          issuerSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          // Issuer creates offer
          offerCreatedRecord <- issuerSvc.createRecord()
          issuerRecordId = offerCreatedRecord.id
          // Issuer sends offer
          _ <- issuerSvc.markOfferSent(issuerRecordId)
          msg <- ZIO.fromEither(offerCreatedRecord.offerCredentialData.get.makeMessage.asJson.as[Message])
          // Holder receives offer
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(OfferCredential.readFromMessage(msg))
          holderRecordId = offerReceivedRecord.id
          // Holder accepts offer
          offerAcceptedRecord <- holderSvc.acceptCredentialOffer(holderRecordId)
          // Holder sends offer
          _ <- holderSvc.markRequestSent(holderRecordId)
          msg <- ZIO.fromEither(offerAcceptedRecord.get.requestCredentialData.get.makeMessage.asJson.as[Message])
          // Issuer receives request
          requestReceivedRecord <- issuerSvc.receiveCredentialRequest(RequestCredential.readFromMessage(msg))
          // Issuer accepts request
          requestAcceptedRecord <- issuerSvc.acceptCredentialRequest(issuerRecordId)
          // Issuer generates credential
          issue = issueCredential(Some(requestAcceptedRecord.get.thid))
          credentialGenerateRecord <- issuerSvc.markCredentialGenerated(issuerRecordId, issue)
          // Issuer sends credential
          _ <- issuerSvc.markCredentialSent(issuerRecordId)
          msg <- ZIO.fromEither(credentialGenerateRecord.get.issueCredentialData.get.makeMessage.asJson.as[Message])
          // Holder receives credential
          _ <- holderSvc.receiveCredentialIssue(IssueCredential.readFromMessage(msg))
        } yield assertTrue(true)
      }
    ).provideLayer(credentialServiceLayer)
  }

  private[this] def offerCredential(thid: Option[UUID] = Some(UUID.randomUUID())) = OfferCredential(
    from = DidId("did:prism:issuer"),
    to = DidId("did:prism:holder"),
    thid = thid.map(_.toString),
    attachments = Nil,
    body = OfferCredential.Body(
      goal_code = Some("Offer Credential"),
      credential_preview = CredentialPreview(attributes = Seq(Attribute("name", "Alice")))
    )
  )

  private[this] def requestCredential(thid: Option[UUID] = Some(UUID.randomUUID())) = RequestCredential(
    from = DidId("did:prism:holder"),
    to = DidId("did:prism:issuer"),
    thid = thid.map(_.toString),
    attachments = Nil,
    body = RequestCredential.Body()
  )

  private[this] def issueCredential(thid: Option[UUID] = Some(UUID.randomUUID())) = IssueCredential(
    from = DidId("did:prism:issuer"),
    to = DidId("did:prism:holder"),
    thid = thid.map(_.toString),
    attachments = Nil,
    body = IssueCredential.Body()
  )

  extension (svc: CredentialService)
    def createRecord(
        did: DidId = DidId("did:prism:issuer"),
        thid: UUID = UUID.randomUUID(),
        subjectId: String = "did:prism:holder",
        schemaId: Option[String] = None,
        claims: Map[String, String] = Map("name" -> "Alice"),
        validityPeriod: Option[Double] = None,
        automaticIssuance: Option[Boolean] = None,
        awaitConfirmation: Option[Boolean] = None
    ) = {
      svc.createIssueCredentialRecord(
        pairwiseDID = did,
        thid = thid,
        subjectId = subjectId,
        schemaId = schemaId,
        claims = claims,
        validityPeriod = validityPeriod,
        automaticIssuance = automaticIssuance,
        awaitConfirmation = awaitConfirmation
      )
    }

}
