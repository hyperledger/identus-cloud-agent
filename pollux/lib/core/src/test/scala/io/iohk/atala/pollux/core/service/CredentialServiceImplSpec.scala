package io.iohk.atala.pollux.core.service

import io.circe.Json
import io.circe.syntax.*
import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.mercury.model.{DidId, Message}
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.*
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.model.error.CredentialServiceError.*
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.nio.charset.StandardCharsets
import java.util.{Base64, UUID}

object CredentialServiceImplSpec extends ZIOSpecDefault with CredentialServiceSpecHelper {

  override def spec =
    suite("CredentialServiceImpl")(singleWalletSpec, multiWalletSpec).provide(credentialServiceLayer)

  private val singleWalletSpec =
    suite("singleWalletSpec")(
      test("createIssuerCredentialRecord without schema creates a valid issuer credential record") {
        check(
          Gen.option(Gen.double),
          Gen.option(Gen.boolean),
          Gen.option(Gen.boolean)
        ) { (validityPeriod, automaticIssuance, awaitConfirmation) =>
          for {
            svc <- ZIO.service[CredentialService]
            pairwiseIssuerDid = DidId("did:peer:INVITER")
            pairwiseHolderDid = DidId("did:peer:HOLDER")
            thid = DidCommID(UUID.randomUUID().toString())
            record <- svc.createRecord(
              thid = thid,
              pairwiseIssuerDID = pairwiseIssuerDid,
              pairwiseHolderDID = pairwiseHolderDid,
              schemaId = None,
              validityPeriod = validityPeriod,
              automaticIssuance = automaticIssuance,
              awaitConfirmation = awaitConfirmation
            )
          } yield {
            assertTrue(record.thid == thid) &&
            assertTrue(record.updatedAt.isEmpty) &&
            assertTrue(record.schemaId.isEmpty) &&
            assertTrue(record.validityPeriod == validityPeriod) &&
            assertTrue(record.automaticIssuance == automaticIssuance) &&
            assertTrue(record.awaitConfirmation == awaitConfirmation) &&
            assertTrue(record.role == Role.Issuer) &&
            assertTrue(record.protocolState == ProtocolState.OfferPending) &&
            assertTrue(record.publicationState.isEmpty) &&
            assertTrue(record.offerCredentialData.isDefined) &&
            assertTrue(record.offerCredentialData.get.from == pairwiseIssuerDid) &&
            assertTrue(record.offerCredentialData.get.to == pairwiseHolderDid) &&
            // FIXME: update the assertion when when CredentialOffer attachment is realized
            // assertTrue(record.offerCredentialData.get.attachments.isEmpty) &&
            assertTrue(record.offerCredentialData.get.thid.contains(thid.toString)) &&
            assertTrue(record.offerCredentialData.get.body.comment.isEmpty) &&
            assertTrue(record.offerCredentialData.get.body.goal_code.contains("Offer Credential")) &&
            assertTrue(record.offerCredentialData.get.body.multiple_available.isEmpty) &&
            assertTrue(record.offerCredentialData.get.body.replacement_id.isEmpty) &&
            assertTrue(record.offerCredentialData.get.body.formats.isEmpty) &&
            assertTrue(
              record.offerCredentialData.get.body.credential_preview.attributes == Seq(
                Attribute("name", "Alice", None),
                Attribute(
                  "address",
                  Base64.getUrlEncoder.encodeToString(
                    io.circe.parser
                      .parse("""{"street": "Street Name", "number": "12"}""")
                      .getOrElse(Json.Null)
                      .noSpaces
                      .getBytes(StandardCharsets.UTF_8)
                  ),
                  Some("application/json")
                )
              )
            ) &&
            assertTrue(record.requestCredentialData.isEmpty) &&
            assertTrue(record.issueCredentialData.isEmpty) &&
            assertTrue(record.issuedCredentialRaw.isEmpty)
          }
        }
      },
      test("createIssuerCredentialRecord with a schema and valid claims creates a valid issuer credential record") {
        check(
          Gen.option(Gen.double),
          Gen.option(Gen.boolean),
          Gen.option(Gen.boolean)
        ) { (validityPeriod, automaticIssuance, awaitConfirmation) =>
          for {
            svc <- ZIO.service[CredentialService]
            pairwiseIssuerDid = DidId("did:peer:INVITER")
            pairwiseHolderDid = DidId("did:peer:HOLDER")
            claims = io.circe.parser
              .parse("""
                |{
                |   "credentialSubject": {
                |     "emailAddress": "alice@wonderland.com",
                |     "givenName": "Alice",
                |     "familyName": "Wonderland",
                |     "dateOfIssuance": "2000-01-01T10:00:00Z",
                |     "drivingLicenseID": "12345",
                |     "drivingClass": 5
                |   }
                |}
                |""".stripMargin)
              .getOrElse(Json.Null)
            thid = DidCommID(UUID.randomUUID().toString())
            record <- svc.createRecord(
              thid = thid,
              pairwiseIssuerDID = pairwiseIssuerDid,
              pairwiseHolderDID = pairwiseHolderDid,
              schemaId = Some("resource:///vc-schema-example.json"),
              claims = claims,
              validityPeriod = validityPeriod,
              automaticIssuance = automaticIssuance,
              awaitConfirmation = awaitConfirmation
            )
            attributes <- CredentialService.convertJsonClaimsToAttributes(claims)
          } yield {
            assertTrue(record.thid == thid) &&
            assertTrue(record.updatedAt.isEmpty) &&
            assertTrue(
              record.schemaId.contains("resource:///vc-schema-example.json")
            ) &&
            assertTrue(record.validityPeriod == validityPeriod) &&
            assertTrue(record.automaticIssuance == automaticIssuance) &&
            assertTrue(record.awaitConfirmation == awaitConfirmation) &&
            assertTrue(record.role == Role.Issuer) &&
            assertTrue(record.protocolState == ProtocolState.OfferPending) &&
            assertTrue(record.publicationState.isEmpty) &&
            assertTrue(record.offerCredentialData.isDefined) &&
            assertTrue(record.offerCredentialData.get.from == pairwiseIssuerDid) &&
            assertTrue(record.offerCredentialData.get.to == pairwiseHolderDid) &&
            // FIXME: update the assertion when when CredentialOffer attachment is realized
            // assertTrue(record.offerCredentialData.get.attachments.isEmpty) &&
            assertTrue(record.offerCredentialData.get.thid.contains(thid.toString)) &&
            assertTrue(record.offerCredentialData.get.body.comment.isEmpty) &&
            assertTrue(record.offerCredentialData.get.body.goal_code.contains("Offer Credential")) &&
            assertTrue(record.offerCredentialData.get.body.multiple_available.isEmpty) &&
            assertTrue(record.offerCredentialData.get.body.replacement_id.isEmpty) &&
            assertTrue(record.offerCredentialData.get.body.formats.isEmpty) &&
            assertTrue(record.offerCredentialData.get.body.credential_preview.attributes == attributes) &&
            assertTrue(record.requestCredentialData.isEmpty) &&
            assertTrue(record.issueCredentialData.isEmpty) &&
            assertTrue(record.issuedCredentialRaw.isEmpty)
          }
        }
      },
      test("createIssuerCredentialRecord with a schema and invalid claims should fail") {
        check(
          Gen.option(Gen.double),
          Gen.option(Gen.boolean),
          Gen.option(Gen.boolean)
        ) { (validityPeriod, automaticIssuance, awaitConfirmation) =>
          for {
            svc <- ZIO.service[CredentialService]
            pairwiseIssuerDid = DidId("did:peer:INVITER")
            pairwiseHolderDid = DidId("did:peer:HOLDER")
            claims = io.circe.parser
              .parse(
                """
                |{
                |   "emailAddress": "alice@wonderland.com",
                |   "givenName": "Alice",
                |   "familyName": "Wonderland"
                |}
                |""".stripMargin
              )
              .getOrElse(Json.Null)
            thid = DidCommID(UUID.randomUUID().toString())
            record <- svc
              .createRecord(
                thid = thid,
                pairwiseIssuerDID = pairwiseIssuerDid,
                pairwiseHolderDID = pairwiseHolderDid,
                schemaId = Some("resource:///vc-schema-example.json"),
                claims = claims,
                validityPeriod = validityPeriod,
                automaticIssuance = automaticIssuance,
                awaitConfirmation = awaitConfirmation
              )
              .exit
          } yield {
            assertTrue(record match
              case Exit.Failure(Cause.Fail(_: CredentialServiceError, _)) => true
              case _                                                      => false
            )
          }
        }
      },
      test("createIssuerCredentialRecord should reject creation with a duplicate 'thid'") {
        for {
          svc <- ZIO.service[CredentialService]
          thid = DidCommID()
          aRecord <- svc.createRecord(thid = thid)
          bRecord <- svc.createRecord(thid = thid).exit
        } yield {
          assertTrue(bRecord match
            case Exit.Failure(Cause.Fail(_: RepositoryError, _)) => true
            case _                                               => false
          )
        }
      },
      test("getCredentialRecords returns the created records") {
        for {
          svc <- ZIO.service[CredentialService]
          aRecord <- svc.createRecord()
          bRecord <- svc.createRecord()
          records <- svc.getIssueCredentialRecords(false).map(_._1)
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
          records <- svc.getIssueCredentialRecordsByStates(
            ignoreWithZeroRetries = true,
            limit = 10,
            ProtocolState.OfferPending
          )
          onePending = assertTrue(records.size == 1) && assertTrue(records.contains(aRecord))
          records <- svc.getIssueCredentialRecordsByStates(
            ignoreWithZeroRetries = true,
            limit = 10,
            ProtocolState.OfferSent
          )
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
          record <- svc.getIssueCredentialRecord(DidCommID())
        } yield assertTrue(record.isEmpty)
      },
      test("receiveCredentialOffer successfully creates a record") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          subjectId = "did:prism:subject"
          offer = offerCredential()
          holderRecord <- holderSvc.receiveCredentialOffer(offer)
        } yield {
          assertTrue(holderRecord.thid.toString == offer.thid.get) &&
          assertTrue(holderRecord.updatedAt.isEmpty) &&
          assertTrue(holderRecord.schemaId.isEmpty) &&
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
            case Exit.Failure(Cause.Fail(_: RepositoryError, _)) => true
            case _                                               => false
          )
        }
      },
      test("acceptCredentialOffer updates the record's protocol state") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          subjectId = "did:prism:60821d6833158c93fde5bb6a40d69996a683bf1fa5cdf32c458395b2887597c3"
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(offer)
          offerAcceptedRecord <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id, subjectId)
        } yield {
          assertTrue(offerAcceptedRecord.protocolState == ProtocolState.RequestPending) &&
          assertTrue(offerAcceptedRecord.offerCredentialData.isDefined) &&
          assertTrue(offerAcceptedRecord.subjectId.contains(subjectId)) &&
          assertTrue(offerAcceptedRecord.offerCredentialData.get.from == offer.from) &&
          assertTrue(offerAcceptedRecord.offerCredentialData.get.to == offer.to) &&
          assertTrue(offerAcceptedRecord.offerCredentialData.get.attachments == offer.attachments) &&
          assertTrue(offerAcceptedRecord.offerCredentialData.get.thid == offer.thid) &&
          assertTrue(offerAcceptedRecord.offerCredentialData.get.body.comment.isEmpty) &&
          assertTrue(offerAcceptedRecord.offerCredentialData.get.body.goal_code.contains("Offer Credential")) &&
          assertTrue(offerAcceptedRecord.offerCredentialData.get.body.multiple_available.isEmpty) &&
          assertTrue(offerAcceptedRecord.offerCredentialData.get.body.replacement_id.isEmpty) &&
          assertTrue(offerAcceptedRecord.offerCredentialData.get.body.formats.isEmpty) &&
          assertTrue(
            offerAcceptedRecord.offerCredentialData.get.body.credential_preview.attributes == Seq(
              Attribute("name", "Alice", None)
            )
          )
        }
      },
      test("acceptCredentialOffer cannot be called twice for the same record") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          subjectId = "did:prism:60821d6833158c93fde5bb6a40d69996a683bf1fa5cdf32c458395b2887597c3"
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(offer)
          _ <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id, subjectId)
          exit <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id, subjectId).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(Cause.Fail(_: InvalidFlowStateError, _)) => true
            case _                                                     => false
          )
        }
      },
      test("acceptCredentialOffer should reject unsupported `subjectId` format") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          subjectId = "did:unknown:subject"
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(offer)
          record <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id, subjectId).exit
        } yield {
          assertTrue(record match
            case Exit.Failure(Cause.Fail(_: UnsupportedDidFormat, _)) => true
            case _                                                    => false
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
          assertTrue(requestReceivedRecord.protocolState == ProtocolState.RequestReceived) &&
          assertTrue(requestReceivedRecord.requestCredentialData.contains(request))
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
            case Exit.Failure(Cause.Fail(_: InvalidFlowStateError, _)) => true
            case _                                                     => false
          )
        }
      },
      test("receiveCredentialRequest is rejected for an unknown 'thid'") {
        for {
          issuerSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          issuerRecord <- issuerSvc.createRecord()
          _ <- issuerSvc.markOfferSent(issuerRecord.id)
          request = requestCredential(Some(DidCommID()))
          exit <- issuerSvc.receiveCredentialRequest(request).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(Cause.Fail(_: ThreadIdNotFound, _)) => true
            case _                                                => false
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
          requestAcceptedRecord <- issuerSvc.acceptCredentialRequest(requestReceivedRecord.id)
        } yield {
          assertTrue(requestAcceptedRecord.protocolState == ProtocolState.CredentialPending) &&
          assertTrue(requestAcceptedRecord.issueCredentialData.isDefined)
        }
      },
      test("acceptCredentialRequest cannot be called twice for the same record") {
        for {
          issuerSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          issuerRecord <- issuerSvc.createRecord()
          _ <- issuerSvc.markOfferSent(issuerRecord.id)
          request = requestCredential(Some(issuerRecord.thid))
          requestReceivedRecord <- issuerSvc.receiveCredentialRequest(request)
          requestAcceptedRecord <- issuerSvc.acceptCredentialRequest(requestReceivedRecord.id)
          exit <- issuerSvc.acceptCredentialRequest(requestReceivedRecord.id).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(Cause.Fail(_: InvalidFlowStateError, _)) => true
            case _                                                     => false
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
          requestAcceptedRecord <- issuerSvc.acceptCredentialRequest(requestReceivedRecord.id)
          issue = issueCredential()
          credentialGeneratedRecord <- issuerSvc.markCredentialGenerated(issuerRecord.id, issue)
        } yield {
          assertTrue(credentialGeneratedRecord.protocolState == ProtocolState.CredentialGenerated) &&
          assertTrue(credentialGeneratedRecord.issueCredentialData.contains(issue))
        }
      },
      test("receiveCredentialIssue successfully updates the record") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          subjectId = "did:prism:60821d6833158c93fde5bb6a40d69996a683bf1fa5cdf32c458395b2887597c3"
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(offer)
          _ <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id, subjectId)
          _ <- holderSvc.generateCredentialRequest(offerReceivedRecord.id, JWT("Fake JWT"))
          _ <- holderSvc.markRequestSent(offerReceivedRecord.id)
          issue = issueCredential(thid = Some(offerReceivedRecord.thid))
          credentialReceivedRecord <- holderSvc.receiveCredentialIssue(issue)
        } yield {
          assertTrue(credentialReceivedRecord.protocolState == ProtocolState.CredentialReceived) &&
          assertTrue(credentialReceivedRecord.issueCredentialData.contains(issue))
        }
      },
      test("receiveCredentialIssue cannot be called twice for the same record") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          subjectId = "did:prism:60821d6833158c93fde5bb6a40d69996a683bf1fa5cdf32c458395b2887597c3"
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(offer)
          _ <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id, subjectId)
          _ <- holderSvc.generateCredentialRequest(offerReceivedRecord.id, JWT("Fake JWT"))
          _ <- holderSvc.markRequestSent(offerReceivedRecord.id)
          issue = issueCredential(thid = Some(offerReceivedRecord.thid))
          credentialReceivedRecord <- holderSvc.receiveCredentialIssue(issue)
          exit <- holderSvc.receiveCredentialIssue(issue).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(Cause.Fail(_: InvalidFlowStateError, _)) => true
            case _                                                     => false
          )
        }
      },
      test("receiveCredentialIssue is rejected for an unknown 'thid'") {
        for {
          holderSvc <- ZIO.service[CredentialService].provideLayer(credentialServiceLayer)
          offer = offerCredential()
          subjectId = "did:prism:60821d6833158c93fde5bb6a40d69996a683bf1fa5cdf32c458395b2887597c3"
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(offer)
          _ <- holderSvc.acceptCredentialOffer(offerReceivedRecord.id, subjectId)
          _ <- holderSvc.generateCredentialRequest(offerReceivedRecord.id, JWT("Fake JWT"))
          _ <- holderSvc.markRequestSent(offerReceivedRecord.id)
          issue = issueCredential(thid = Some(DidCommID()))
          exit <- holderSvc.receiveCredentialIssue(issue).exit
        } yield {
          assertTrue(exit match
            case Exit.Failure(Cause.Fail(_: ThreadIdNotFound, _)) => true
            case _                                                => false
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
          subjectId = "did:prism:60821d6833158c93fde5bb6a40d69996a683bf1fa5cdf32c458395b2887597c3"
          // Holder accepts offer
          _ <- holderSvc.acceptCredentialOffer(holderRecordId, subjectId)
          // Holder generates proof
          requestGeneratedRecord <- holderSvc.generateCredentialRequest(offerReceivedRecord.id, JWT("Fake JWT"))
          // Holder sends offer
          _ <- holderSvc.markRequestSent(holderRecordId)
          msg <- ZIO.fromEither(requestGeneratedRecord.requestCredentialData.get.makeMessage.asJson.as[Message])
          // Issuer receives request
          requestReceivedRecord <- issuerSvc.receiveCredentialRequest(RequestCredential.readFromMessage(msg))
          // Issuer accepts request
          requestAcceptedRecord <- issuerSvc.acceptCredentialRequest(issuerRecordId)
          // Issuer generates credential
          issue = issueCredential(Some(requestAcceptedRecord.thid))
          credentialGenerateRecord <- issuerSvc.markCredentialGenerated(issuerRecordId, issue)
          // Issuer sends credential
          _ <- issuerSvc.markCredentialSent(issuerRecordId)
          msg <- ZIO.fromEither(credentialGenerateRecord.issueCredentialData.get.makeMessage.asJson.as[Message])
          // Holder receives credential
          _ <- holderSvc.receiveCredentialIssue(IssueCredential.readFromMessage(msg))
        } yield assertTrue(true)
      }
    ).provideSomeLayer(ZLayer.succeed(WalletAccessContext(WalletId.random)))

  private val multiWalletSpec =
    suite("multi-wallet spec")(
      test("createIssueCredentialRecord for different wallet and isolate records") {
        val walletId1 = WalletId.random
        val walletId2 = WalletId.random
        val wallet1 = ZLayer.succeed(WalletAccessContext(walletId1))
        val wallet2 = ZLayer.succeed(WalletAccessContext(walletId2))
        for {
          svc <- ZIO.service[CredentialService]
          record1 <- svc.createRecord().provide(wallet1)
          record2 <- svc.createRecord().provide(wallet2)
          ownRecord1 <- svc.getIssueCredentialRecord(record1.id).provide(wallet1)
          ownRecord2 <- svc.getIssueCredentialRecord(record2.id).provide(wallet2)
          crossRecord1 <- svc.getIssueCredentialRecord(record1.id).provide(wallet2)
          crossRecord2 <- svc.getIssueCredentialRecord(record2.id).provide(wallet1)
        } yield assert(ownRecord1)(isSome(equalTo(record1))) &&
          assert(ownRecord2)(isSome(equalTo(record2))) &&
          assert(crossRecord1)(isNone) &&
          assert(crossRecord2)(isNone)
      }
    )

}
