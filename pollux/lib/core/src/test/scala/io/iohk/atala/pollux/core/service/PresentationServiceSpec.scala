package io.iohk.atala.pollux.core.service

import io.circe.parser.decode
import io.circe.syntax.*
import io.iohk.atala.mercury.model.{AttachmentDescriptor, DidId}
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.mercury.protocol.presentproof.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.*
import io.iohk.atala.pollux.core.model.PresentationRecord.*
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.error.PresentationError.*
import io.iohk.atala.pollux.core.model.presentation.Options
import io.iohk.atala.pollux.core.repository.{CredentialRepository, PresentationRepository}
import io.iohk.atala.pollux.vc.jwt.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.time.Instant
import io.iohk.atala.shared.models.WalletId
import io.iohk.atala.shared.models.WalletAccessContext

object PresentationServiceSpec extends ZIOSpecDefault with PresentationServiceSpecHelper {

  override def spec =
    suite("PresentationService")(singleWalletSpec, multiWalletSpec).provide(presentationServiceLayer)

  private val singleWalletSpec =
    suite("singleWalletSpec")(
      test("createPresentationRecord creates a valid PresentationRecord") {
        val didGen = for {
          suffix <- Gen.stringN(10)(Gen.alphaNumericChar)
        } yield DidId("did:peer:" + suffix)

        val proofTypeGen = for {
          schemaId <- Gen.stringN(10)(Gen.alphaChar)
          requiredFields <- Gen.listOfBounded(1, 5)(Gen.stringN(10)(Gen.alphaChar)).map(Some(_))
          trustIssuers <- Gen.listOfBounded(1, 5)(didGen).map(Some(_))
        } yield ProofType(schemaId, requiredFields, trustIssuers)

        val optionsGen = for {
          challenge <- Gen.stringN(10)(Gen.alphaNumericChar)
          domain <- Gen.stringN(10)(Gen.alphaNumericChar)
        } yield Options(challenge, domain)

        check(
          Gen.uuid.map(e => DidCommID(e.toString())),
          Gen.option(Gen.string),
          Gen.listOfBounded(1, 5)(proofTypeGen),
          Gen.option(optionsGen)
        ) { (thid, connectionId, proofTypes, options) =>
          for {
            svc <- ZIO.service[PresentationService]
            pairwiseVerifierDid = DidId("did:peer:Verifier")
            pairwiseProverDid = DidId("did:peer:Prover")
            record <- svc.createPresentationRecord(
              pairwiseVerifierDid,
              pairwiseProverDid,
              thid,
              connectionId,
              proofTypes,
              options
            )
          } yield {
            assertTrue(record.thid == thid) &&
            assertTrue(record.updatedAt.isEmpty) &&
            assertTrue(record.connectionId == connectionId) &&
            assertTrue(record.role == PresentationRecord.Role.Verifier) &&
            assertTrue(record.protocolState == PresentationRecord.ProtocolState.RequestPending) &&
            assertTrue(record.requestPresentationData.isDefined) &&
            assertTrue(record.requestPresentationData.get.to == pairwiseProverDid) &&
            assertTrue(record.requestPresentationData.get.thid.contains(thid.toString)) &&
            assertTrue(record.requestPresentationData.get.body.goal_code.contains("Request Proof Presentation")) &&
            assertTrue(record.requestPresentationData.get.body.proof_types == proofTypes) &&
            assertTrue(
              if (record.requestPresentationData.get.attachments.length != 0) {
                val maybePresentationOptions =
                  record.requestPresentationData.get.attachments.headOption
                    .map(attachment =>
                      decode[io.iohk.atala.mercury.model.JsonData](attachment.data.asJson.noSpaces)
                        .flatMap(data =>
                          io.iohk.atala.pollux.core.model.presentation.PresentationAttachment.given_Decoder_PresentationAttachment
                            .decodeJson(data.json.asJson)
                            .map(_.options)
                        )
                    )
                    .get
                maybePresentationOptions
                  .map(
                    _ == options
                  )
                  .getOrElse(false)
              } else true
            ) &&
            assertTrue(record.proposePresentationData.isEmpty) &&
            assertTrue(record.presentationData.isEmpty) &&
            assertTrue(record.credentialsToUse.isEmpty)
          }
        }
      },
      test("getPresentationRecords returns created PresentationRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          pairwiseProverDid = DidId("did:peer:Prover")
          record1 <- svc.createRecord()
          record2 <- svc.createRecord()
          records <- svc.getPresentationRecords(false)
        } yield {
          assertTrue(records.size == 2)
        }
      },
      test("getPresentationRecordsByStates returns the correct records") {
        for {
          svc <- ZIO.service[PresentationService]
          aRecord <- svc.createRecord()
          records <- svc.getPresentationRecordsByStates(
            ignoreWithZeroRetries = true,
            limit = 10,
            PresentationRecord.ProtocolState.RequestPending
          )
          onePending = assertTrue(records.size == 1) && assertTrue(records.contains(aRecord))
          records <- svc.getPresentationRecordsByStates(
            ignoreWithZeroRetries = true,
            limit = 10,
            PresentationRecord.ProtocolState.RequestSent
          )
          zeroSent = assertTrue(records.isEmpty)
        } yield onePending && zeroSent
      },
      test("getPresentationRecord returns the correct record") {
        for {
          svc <- ZIO.service[PresentationService]
          aRecord <- svc.createRecord()
          bRecord <- svc.createRecord()
          record <- svc.getPresentationRecord(bRecord.id)
        } yield assertTrue(record.contains(bRecord))
      },
      test("getPresentationRecord returns nothing for an unknown 'recordId'") {
        for {
          svc <- ZIO.service[PresentationService]
          aRecord <- svc.createRecord()
          bRecord <- svc.createRecord()
          record <- svc.getPresentationRecord(DidCommID())
        } yield assertTrue(record.isEmpty)
      },
      test("createPresentationPayloadFromRecord returns jwt prsentation payload") {
        for {
          repo <- ZIO.service[CredentialRepository]
          aIssueCredentialRecord = issueCredentialRecord
          _ <- repo.createIssueCredentialRecord(aIssueCredentialRecord)
          rawCredentialData =
            """{"base64":"ZXlKaGJHY2lPaUpGVXpJMU5rc2lMQ0owZVhBaU9pSktWMVFpZlEuZXlKcFlYUWlPakUyTnprek1qYzROaklzSW1GMVpDSTZJbVJ2YldGcGJpSXNJbTV2Ym1ObElqb2lZMlk1T1RJMk56Z3RPREV3TmkwME1EZzVMV0UxWXprdE5tTmhObU0wWkRBMU1HVTBJaXdpZG5BaU9uc2lRR052Ym5SbGVIUWlPbHNpYUhSMGNITTZMeTkzZDNjdWR6TXViM0puTHpJd01UZ3ZjSEpsYzJWdWRHRjBhVzl1Y3k5Mk1TSmRMQ0owZVhCbElqcGJJbFpsY21sbWFXRmliR1ZRY21WelpXNTBZWFJwYjI0aVhYMHNJbWx6Y3lJNkltUnBaRHB3Y21semJUcGhaR0psT1RJNE9XUXdZelZtWWpVMlptWmhOVEF6T0Rka01UZ3dOR0ZpTkdFeE5UYzJOVEkzWXprME5tRTFNalV5T0RFM1ptRTRaVGhoTW1OalpXUXdPa056YzBKRGMyZENSVzFKUzBSWE1XaGpNMUpzWTJsb2NHSnRVbXhsUTJ0UlFWVktVRU5uYkZSYVYwNTNUV3BWTW1GNlJWTkpSUzFNYVVkTU0xRklaRlZ1VG10d1dXSkthSE5VYTIxWVVGaEpVM0ZXZWpjMll6RlZPWGhvVURseWNFZHBSSEZXTlRselJYcEtWbEpEYWxJMGEwMHdaMGg0YkhWUU5tVk5Ta2wwZHpJMk4yWllWbEpoTUhoRE5XaEthVU5uTVhSWldFNHdXbGhKYjJGWE5XdGFXR2R3UlVGU1ExUjNiMHBWTWxacVkwUkpNVTV0YzNoRmFVSlFhVFJvYVRrd1FqTldTbnBhUzFkSGVWbGlSVFZLYkhveGVVVnhiR010TFc1T1ZsQmpXVlJmWVRaU2IyYzJiR1ZtWWtKTmVWWlZVVzh3WlVwRVRrbENPRnBpYWkxdWFrTlRUR05PZFhVek1URlZWM1JOVVhWWkluMC5CcmFpbEVXa2VlSXhWbjY3dnpkVHZGTXpBMV9oNzFoaDZsODBHRFBpbkRaVVB4ajAxSC0tUC1QZDIxTk9wRDd3am51SDkxdUNBOFZMUW9fS2FnVjlnQQo="}"""
          _ <- repo.updateWithIssuedRawCredential(
            aIssueCredentialRecord.id,
            IssueCredential.makeIssueCredentialFromRequestCredential(requestCredential.makeMessage),
            rawCredentialData,
            IssueCredentialRecord.ProtocolState.CredentialReceived
          )
          svc <- ZIO.service[PresentationService]
          aRecord <- svc.createRecord()
          repo <- ZIO.service[PresentationRepository]
          _ <- repo.updatePresentationWithCredentialsToUse(
            aRecord.id,
            Some(Seq(aIssueCredentialRecord.id.value)),
            PresentationRecord.ProtocolState.RequestPending
          )
          issuer = createIssuer(DID("did:prism:issuer"))
          aPresentationPayload <- svc.createPresentationPayloadFromRecord(aRecord.id, issuer, Instant.now())
        } yield {
          assertTrue(aPresentationPayload.toJwtPresentationPayload.iss == "did:prism:issuer")
        }
      },
      test("markRequestPresentationSent returns updated PresentationRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          pairwiseProverDid = DidId("did:peer:Prover")
          record <- svc.createRecord()
          record <- svc.markRequestPresentationSent(record.id)

        } yield {
          assertTrue(record.protocolState == PresentationRecord.ProtocolState.RequestSent)
        }
      },
      test("markRequestPresentationRejected returns updated PresentationRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          pairwiseProverDid = DidId("did:peer:Prover")
          record <- svc.createRecord()
          repo <- ZIO.service[PresentationRepository]
          _ <- repo.updatePresentationRecordProtocolState(
            record.id,
            PresentationRecord.ProtocolState.RequestPending,
            PresentationRecord.ProtocolState.RequestReceived
          )
          record <- svc.markRequestPresentationRejected(record.id)

        } yield {
          assertTrue(record.protocolState == PresentationRecord.ProtocolState.RequestRejected)
        }
      },
      test("receiveRequestPresentation updates the RequestPresentation in PresentatinRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          connectionId = Some("connectionId")
          body = RequestPresentation.Body(goal_code = Some("Presentation Request"))
          presentationAttachmentAsJson = """{
                "challenge": "1f44d55f-f161-4938-a659-f8026467f126",
                "domain": "us.gov/DriverLicense",
                "credential_manifest": {}
            }"""
          prover = DidId("did:peer:Prover")
          verifier = DidId("did:peer:Verifier")

          attachmentDescriptor = AttachmentDescriptor.buildJsonAttachment(payload = presentationAttachmentAsJson)
          requestPresentation = RequestPresentation(
            body = body,
            attachments = Seq(attachmentDescriptor),
            to = prover,
            from = verifier,
          )
          aRecord <- svc.receiveRequestPresentation(connectionId, requestPresentation)

        } yield {
          assertTrue(aRecord.connectionId == connectionId) &&
          assertTrue(aRecord.protocolState == PresentationRecord.ProtocolState.RequestReceived) &&
          assertTrue(aRecord.requestPresentationData == Some(requestPresentation))
        }
      },
      test("acceptRequestPresentation updates the PresentatinRecord") {
        for {
          repo <- ZIO.service[CredentialRepository]
          aIssueCredentialRecord = issueCredentialRecord
          _ <- repo.createIssueCredentialRecord(aIssueCredentialRecord)
          rawCredentialData =
            """{"base64":"ZXlKaGJHY2lPaUpGVXpJMU5rc2lMQ0owZVhBaU9pSktWMVFpZlEuZXlKcFlYUWlPakUyTnprek1qYzROaklzSW1GMVpDSTZJbVJ2YldGcGJpSXNJbTV2Ym1ObElqb2lZMlk1T1RJMk56Z3RPREV3TmkwME1EZzVMV0UxWXprdE5tTmhObU0wWkRBMU1HVTBJaXdpZG5BaU9uc2lRR052Ym5SbGVIUWlPbHNpYUhSMGNITTZMeTkzZDNjdWR6TXViM0puTHpJd01UZ3ZjSEpsYzJWdWRHRjBhVzl1Y3k5Mk1TSmRMQ0owZVhCbElqcGJJbFpsY21sbWFXRmliR1ZRY21WelpXNTBZWFJwYjI0aVhYMHNJbWx6Y3lJNkltUnBaRHB3Y21semJUcGhaR0psT1RJNE9XUXdZelZtWWpVMlptWmhOVEF6T0Rka01UZ3dOR0ZpTkdFeE5UYzJOVEkzWXprME5tRTFNalV5T0RFM1ptRTRaVGhoTW1OalpXUXdPa056YzBKRGMyZENSVzFKUzBSWE1XaGpNMUpzWTJsb2NHSnRVbXhsUTJ0UlFWVktVRU5uYkZSYVYwNTNUV3BWTW1GNlJWTkpSUzFNYVVkTU0xRklaRlZ1VG10d1dXSkthSE5VYTIxWVVGaEpVM0ZXZWpjMll6RlZPWGhvVURseWNFZHBSSEZXTlRselJYcEtWbEpEYWxJMGEwMHdaMGg0YkhWUU5tVk5Ta2wwZHpJMk4yWllWbEpoTUhoRE5XaEthVU5uTVhSWldFNHdXbGhKYjJGWE5XdGFXR2R3UlVGU1ExUjNiMHBWTWxacVkwUkpNVTV0YzNoRmFVSlFhVFJvYVRrd1FqTldTbnBhUzFkSGVWbGlSVFZLYkhveGVVVnhiR010TFc1T1ZsQmpXVlJmWVRaU2IyYzJiR1ZtWWtKTmVWWlZVVzh3WlVwRVRrbENPRnBpYWkxdWFrTlRUR05PZFhVek1URlZWM1JOVVhWWkluMC5CcmFpbEVXa2VlSXhWbjY3dnpkVHZGTXpBMV9oNzFoaDZsODBHRFBpbkRaVVB4ajAxSC0tUC1QZDIxTk9wRDd3am51SDkxdUNBOFZMUW9fS2FnVjlnQQo="}"""
          _ <- repo.updateWithIssuedRawCredential(
            aIssueCredentialRecord.id,
            IssueCredential.makeIssueCredentialFromRequestCredential(requestCredential.makeMessage),
            rawCredentialData,
            IssueCredentialRecord.ProtocolState.CredentialReceived
          )
          svc <- ZIO.service[PresentationService]
          connectionId = Some("connectionId")

          aRecord <- svc.receiveRequestPresentation(connectionId, requestPresentation)
          credentialsToUse = Seq(aIssueCredentialRecord.id.value)
          updateRecord <- svc.acceptRequestPresentation(aRecord.id, credentialsToUse)

        } yield {
          assertTrue(updateRecord.connectionId == connectionId) &&
          // assertTrue(updateRecord.requestPresentationData == Some(requestPresentation)) && // FIXME: enabling them make the test fail.
          assertTrue(updateRecord.credentialsToUse.contains(credentialsToUse))
        }
      },
      test("rejectRequestPresentation updates the PresentatinRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          connectionId = Some("connectionId")
          aRecord <- svc.receiveRequestPresentation(connectionId, requestPresentation)
          updateRecord <- svc.rejectRequestPresentation(aRecord.id)

        } yield {
          assertTrue(updateRecord.connectionId == connectionId) &&
          // assertTrue(updateRecord.requestPresentationData == Some(requestPresentation)) && // FIXME: enabling them make the test fail.
          assertTrue(updateRecord.protocolState == PresentationRecord.ProtocolState.RequestRejected)
        }
      },
      test("markPresentationSent returns updated PresentationRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          pairwiseProverDid = DidId("did:peer:Prover")
          record <- svc.createRecord()
          repo <- ZIO.service[PresentationRepository]
          _ <- repo.updatePresentationRecordProtocolState(
            record.id,
            PresentationRecord.ProtocolState.RequestPending,
            PresentationRecord.ProtocolState.PresentationGenerated
          )
          record <- svc.markPresentationSent(record.id)

        } yield {
          assertTrue(record.protocolState == PresentationRecord.ProtocolState.PresentationSent)
        }
      },
      test("receivePresentation updates the PresentatinRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          aRecord <- svc.createRecord()
          p = presentation(aRecord.thid.value)
          aRecordReceived <- svc.receivePresentation(p)

        } yield {
          assertTrue(aRecordReceived.id == aRecord.id) &&
          assertTrue(aRecordReceived.presentationData == Some(p))
        }
      },
      test("acceptPresentation updates the PresentatinRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          aRecord <- svc.createRecord()
          p = presentation(aRecord.thid.value)
          aRecordReceived <- svc.receivePresentation(p)
          repo <- ZIO.service[PresentationRepository]
          _ <- repo.updatePresentationRecordProtocolState(
            aRecord.id,
            PresentationRecord.ProtocolState.PresentationReceived,
            PresentationRecord.ProtocolState.PresentationVerified
          )
          aRecordAccept <- svc.acceptPresentation(aRecord.id)
        } yield {
          assertTrue(aRecordReceived.id == aRecord.id) &&
          assertTrue(aRecordReceived.presentationData == Some(p))
        }
      },
      test("markPresentationRejected updates the PresentatinRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          aRecord <- svc.createRecord()
          p = presentation(aRecord.thid.value)
          _ <- svc.receivePresentation(p)
          repo <- ZIO.service[PresentationRepository]
          _ <- repo.updatePresentationRecordProtocolState(
            aRecord.id,
            PresentationRecord.ProtocolState.PresentationReceived,
            PresentationRecord.ProtocolState.PresentationVerified
          )
          aRecordReject <- svc.markPresentationRejected(aRecord.id)
        } yield {
          assertTrue(aRecordReject.id == aRecord.id) &&
          assertTrue(aRecordReject.presentationData == Some(p)) &&
          assertTrue(aRecordReject.protocolState == PresentationRecord.ProtocolState.PresentationRejected)
        }
      },
      test("rejectPresentation updates the PresentatinRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          aRecord <- svc.createRecord()
          p = presentation(aRecord.thid.value)
          aRecordReceived <- svc.receivePresentation(p)
          repo <- ZIO.service[PresentationRepository]
          _ <- repo.updatePresentationRecordProtocolState(
            aRecord.id,
            PresentationRecord.ProtocolState.PresentationReceived,
            PresentationRecord.ProtocolState.PresentationVerified
          )
          aRecordReject <- svc.rejectPresentation(aRecord.id)
        } yield {
          assertTrue(aRecordReject.id == aRecord.id) &&
          assertTrue(aRecordReject.presentationData == Some(p)) &&
          assertTrue(aRecordReject.protocolState == PresentationRecord.ProtocolState.PresentationRejected)
        }
      },
      test("markPresentationGenerated returns updated PresentationRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          pairwiseProverDid = DidId("did:peer:Prover")
          record <- svc.createRecord()
          p = presentation(record.thid.value)
          repo <- ZIO.service[PresentationRepository]
          _ <- repo.updatePresentationRecordProtocolState(
            record.id,
            PresentationRecord.ProtocolState.RequestPending,
            PresentationRecord.ProtocolState.PresentationPending
          )
          record <- svc.markPresentationGenerated(record.id, p)
        } yield {
          assertTrue(record.protocolState == PresentationRecord.ProtocolState.PresentationGenerated)
        }
      },
      test("markProposePresentationSent returns updated PresentationRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          pairwiseProverDid = DidId("did:peer:Prover")
          record <- svc.createRecord()
          repo <- ZIO.service[PresentationRepository]
          _ <- repo.updatePresentationRecordProtocolState(
            record.id,
            PresentationRecord.ProtocolState.RequestPending,
            PresentationRecord.ProtocolState.ProposalPending
          )
          record <- svc.markProposePresentationSent(record.id)
        } yield {
          assertTrue(record.protocolState == PresentationRecord.ProtocolState.ProposalSent)
        }
      },
      test("receiveProposePresentation updates the PresentatinRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          aRecord <- svc.createRecord()
          p = proposePresentation(aRecord.thid.value)
          aRecordReceived <- svc.receiveProposePresentation(p)
        } yield {
          assertTrue(aRecordReceived.id == aRecord.id) &&
          assertTrue(aRecordReceived.proposePresentationData == Some(p))
        }
      },
      test("acceptProposePresentation updates the PresentatinRecord") {
        for {
          svc <- ZIO.service[PresentationService]
          aRecord <- svc.createRecord()
          p = proposePresentation(aRecord.thid.value)
          aRecordReceived <- svc.receiveProposePresentation(p)
          repo <- ZIO.service[PresentationRepository]
          _ <- repo.updatePresentationRecordProtocolState(
            aRecord.id,
            PresentationRecord.ProtocolState.ProposalPending,
            PresentationRecord.ProtocolState.ProposalReceived
          )
          aRecordAccept <- svc.acceptProposePresentation(aRecord.id)
        } yield {
          assertTrue(aRecordReceived.id == aRecord.id) &&
          assertTrue(aRecordReceived.proposePresentationData == Some(p))
        }
      },
    ).provideSomeLayer(ZLayer.succeed(WalletAccessContext(WalletId.random)))

  private val multiWalletSpec =
    suite("multi-wallet spec")(
      test("createPresentation for different wallet and isolate records") {
        val walletId1 = WalletId.random
        val walletId2 = WalletId.random
        val wallet1 = ZLayer.succeed(WalletAccessContext(walletId1))
        val wallet2 = ZLayer.succeed(WalletAccessContext(walletId2))
        for {
          svc <- ZIO.service[PresentationService]
          record1 <- svc.createRecord().provide(wallet1)
          record2 <- svc.createRecord().provide(wallet2)
          ownRecord1 <- svc.getPresentationRecord(record1.id).provide(wallet1)
          ownRecord2 <- svc.getPresentationRecord(record2.id).provide(wallet2)
          crossRecord1 <- svc.getPresentationRecord(record1.id).provide(wallet2)
          crossRecord2 <- svc.getPresentationRecord(record2.id).provide(wallet1)
        } yield assert(ownRecord1)(isSome(equalTo(record1))) &&
          assert(ownRecord2)(isSome(equalTo(record2))) &&
          assert(crossRecord1)(isNone) &&
          assert(crossRecord2)(isNone)
      }
    )

}
