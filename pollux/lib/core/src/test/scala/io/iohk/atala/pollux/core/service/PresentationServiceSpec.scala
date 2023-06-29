package io.iohk.atala.pollux.core.service

import cats.syntax.validated
import io.circe.parser.decode
import io.circe.syntax._
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.model.Message
import io.iohk.atala.mercury.protocol.presentproof._
import io.iohk.atala.pollux.core.model._
import io.iohk.atala.pollux.core.model.IssueCredentialRecord._
import io.iohk.atala.pollux.core.model.PresentationRecord._
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.error.PresentationError._
import io.iohk.atala.pollux.core.repository.PresentationRepositoryInMemory
import io.iohk.atala.pollux.core.repository.PresentationRepository
import zio.*
import zio.test.*
import java.util.UUID
import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.pollux.core.model.presentation.Options
import io.iohk.atala.pollux.core.model.presentation.Ldp
import io.iohk.atala.pollux.core.model.presentation.ClaimFormat
import io.iohk.atala.pollux.core.model.presentation.PresentationDefinition
import io.iohk.atala.pollux.vc.jwt._
import io.iohk.atala.pollux.vc.jwt.JwtPresentationPayload
import io.iohk.atala.pollux.core.repository.CredentialRepositoryInMemory
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.mercury.DidAgent
import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.mercury.PeerDID
import io.iohk.atala.mercury.AgentPeerService
import cats.syntax.all._
import cats._, cats.data._, cats.implicits._
import java.time.Instant
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import java.security.*
import java.security.interfaces.{ECPrivateKey, ECPublicKey}
import java.security.spec.*
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*

object PresentationServiceSpec extends ZIOSpecDefault {
  type PresentationEnv = PresentationService with PresentationRepository[Task] with CredentialRepository[Task]

  val peerDidAgentLayer =
    AgentPeerService.makeLayer(PeerDID.makePeerDid(serviceEndpoint = Some("http://localhost:9099")))
  val presentationServiceLayer =
    PresentationRepositoryInMemory.layer ++ CredentialRepositoryInMemory.layer ++ peerDidAgentLayer >>> PresentationServiceImpl.layer
  val presentationEnvLayer =
    PresentationRepositoryInMemory.layer ++ CredentialRepositoryInMemory.layer ++ presentationServiceLayer

  def withEnv[E, A](zio: ZIO[PresentationEnv, E, A]): ZIO[Any, E, A] =
    zio.provideLayer(presentationEnvLayer)

  override def spec = {
    suite("PresentationService")(
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
            svc <- ZIO.service[PresentationService].provideLayer(presentationServiceLayer)
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
          svc <- ZIO.service[PresentationService].provideLayer(presentationServiceLayer)
          pairwiseProverDid = DidId("did:peer:Prover")
          record1 <- svc.createRecord()
          record2 <- svc.createRecord()
          records <- svc.getPresentationRecords()

        } yield {
          assertTrue(records.size == 2)
        }
      },
      test("getPresentationRecordsByStates returns the correct records") {
        for {
          svc <- ZIO.service[PresentationService].provideLayer(presentationServiceLayer)
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
          svc <- ZIO.service[PresentationService].provideLayer(presentationServiceLayer)
          aRecord <- svc.createRecord()
          bRecord <- svc.createRecord()
          record <- svc.getPresentationRecord(bRecord.id)
        } yield assertTrue(record.contains(bRecord))
      },
      test("getPresentationRecord returns nothing for an unknown 'recordId'") {
        for {
          svc <- ZIO.service[PresentationService].provideLayer(presentationServiceLayer)
          aRecord <- svc.createRecord()
          bRecord <- svc.createRecord()
          record <- svc.getPresentationRecord(DidCommID())
        } yield assertTrue(record.isEmpty)
      },
      test("createPresentationPayloadFromRecord returns jwt prsentation payload") {
        withEnv(
          for {
            repo <- ZIO.service[CredentialRepository[Task]]
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
            repo <- ZIO.service[PresentationRepository[Task]]
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
        )
      },
      test("markRequestPresentationSent returns updated PresentationRecord") {
        for {
          svc <- ZIO.service[PresentationService].provideLayer(presentationServiceLayer)
          pairwiseProverDid = DidId("did:peer:Prover")
          record <- svc.createRecord()
          record <- svc.markRequestPresentationSent(record.id)

        } yield {
          assertTrue(record.protocolState == PresentationRecord.ProtocolState.RequestSent)
        }
      },
      test("markRequestPresentationRejected returns updated PresentationRecord") {
        withEnv(for {
          svc <- ZIO.service[PresentationService]
          pairwiseProverDid = DidId("did:peer:Prover")
          record <- svc.createRecord()
          repo <- ZIO.service[PresentationRepository[Task]]
          _ <- repo.updatePresentationRecordProtocolState(
            record.id,
            PresentationRecord.ProtocolState.RequestPending,
            PresentationRecord.ProtocolState.RequestReceived
          )
          record <- svc.markRequestPresentationRejected(record.id)

        } yield {
          assertTrue(record.protocolState == PresentationRecord.ProtocolState.RequestRejected)
        })
      },
      test("receiveRequestPresentation updates the RequestPresentation in PresentatinRecord") {
        withEnv(
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
            assertTrue(aRecord.connectionId == connectionId)
            assertTrue(aRecord.protocolState == PresentationRecord.ProtocolState.RequestReceived)
            assertTrue(aRecord.requestPresentationData == Some(requestPresentation))
          }
        )
      },
      test("acceptRequestPresentation updates the PresentatinRecord") {
        withEnv(
          for {
            repo <- ZIO.service[CredentialRepository[Task]]
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
            assertTrue(updateRecord.connectionId == connectionId)
            assertTrue(updateRecord.requestPresentationData == Some(requestPresentation))
            assertTrue(updateRecord.credentialsToUse.contains(credentialsToUse))

          }
        )
      },
      test("rejectRequestPresentation updates the PresentatinRecord") {
        withEnv(
          for {
            svc <- ZIO.service[PresentationService]
            connectionId = Some("connectionId")
            aRecord <- svc.receiveRequestPresentation(connectionId, requestPresentation)
            updateRecord <- svc.rejectRequestPresentation(aRecord.id)

          } yield {
            assertTrue(updateRecord.connectionId == connectionId)
            assertTrue(updateRecord.requestPresentationData == Some(requestPresentation))
            assertTrue(updateRecord.protocolState == PresentationRecord.ProtocolState.RequestRejected)
          }
        )
      },
      test("markPresentationSent returns updated PresentationRecord") {
        withEnv(for {
          svc <- ZIO.service[PresentationService]
          pairwiseProverDid = DidId("did:peer:Prover")
          record <- svc.createRecord()
          repo <- ZIO.service[PresentationRepository[Task]]
          _ <- repo.updatePresentationRecordProtocolState(
            record.id,
            PresentationRecord.ProtocolState.RequestPending,
            PresentationRecord.ProtocolState.PresentationGenerated
          )
          record <- svc.markPresentationSent(record.id)

        } yield {
          assertTrue(record.protocolState == PresentationRecord.ProtocolState.PresentationSent)
        })
      },
      test("receivePresentation updates the PresentatinRecord") {
        withEnv(
          for {
            svc <- ZIO.service[PresentationService]
            aRecord <- svc.createRecord()
            p = presentation(aRecord.thid.value)
            aRecordReceived <- svc.receivePresentation(p)

          } yield {
            assertTrue(aRecordReceived.id == aRecord.id)
            assertTrue(aRecordReceived.presentationData == Some(p))
          }
        )
      },
      test("acceptPresentation updates the PresentatinRecord") {
        withEnv(
          for {
            svc <- ZIO.service[PresentationService]
            aRecord <- svc.createRecord()
            p = presentation(aRecord.thid.value)
            aRecordReceived <- svc.receivePresentation(p)
            repo <- ZIO.service[PresentationRepository[Task]]
            _ <- repo.updatePresentationRecordProtocolState(
              aRecord.id,
              PresentationRecord.ProtocolState.PresentationReceived,
              PresentationRecord.ProtocolState.PresentationVerified
            )
            aRecordAccept <- svc.acceptPresentation(aRecord.id)

          } yield {
            assertTrue(aRecordReceived.id == aRecord.id)
            assertTrue(aRecordReceived.presentationData == Some(p))
          }
        )
      },
      test("markPresentationRejected updates the PresentatinRecord") {
        withEnv(
          for {
            svc <- ZIO.service[PresentationService]
            aRecord <- svc.createRecord()
            p = presentation(aRecord.thid.value)
            aRecordReceived <- svc.receivePresentation(p)
            aRecordAccept <- svc.markPresentationRejected(aRecord.id)

          } yield {
            assertTrue(aRecordAccept.id == aRecord.id)
            assertTrue(aRecordAccept.presentationData == Some(p))
            assertTrue(aRecordAccept.protocolState == PresentationRecord.ProtocolState.PresentationRejected)
          }
        )
      },
      test("rejectPresentation updates the PresentatinRecord") {
        withEnv(
          for {
            svc <- ZIO.service[PresentationService]
            aRecord <- svc.createRecord()
            p = presentation(aRecord.thid.value)
            aRecordReceived <- svc.receivePresentation(p)
            aRecordAccept <- svc.rejectPresentation(aRecord.id)

          } yield {
            assertTrue(aRecordAccept.id == aRecord.id)
            assertTrue(aRecordAccept.presentationData == Some(p))
            assertTrue(aRecordAccept.protocolState == PresentationRecord.ProtocolState.PresentationRejected)
          }
        )
      },
      test("markPresentationGenerated returns updated PresentationRecord") {
        withEnv(for {
          svc <- ZIO.service[PresentationService]
          pairwiseProverDid = DidId("did:peer:Prover")
          record <- svc.createRecord()
          p = presentation(record.thid.value)
          repo <- ZIO.service[PresentationRepository[Task]]
          _ <- repo.updatePresentationRecordProtocolState(
            record.id,
            PresentationRecord.ProtocolState.RequestPending,
            PresentationRecord.ProtocolState.PresentationPending
          )
          record <- svc.markPresentationGenerated(record.id, p)

        } yield {
          assertTrue(record.protocolState == PresentationRecord.ProtocolState.PresentationGenerated)
        })
      },
      test("markProposePresentationSent returns updated PresentationRecord") {
        withEnv(for {
          svc <- ZIO.service[PresentationService]
          pairwiseProverDid = DidId("did:peer:Prover")
          record <- svc.createRecord()
          repo <- ZIO.service[PresentationRepository[Task]]
          _ <- repo.updatePresentationRecordProtocolState(
            record.id,
            PresentationRecord.ProtocolState.RequestPending,
            PresentationRecord.ProtocolState.ProposalPending
          )
          record <- svc.markProposePresentationSent(record.id)

        } yield {
          assertTrue(record.protocolState == PresentationRecord.ProtocolState.ProposalSent)
        })
      },
      test("receiveProposePresentation updates the PresentatinRecord") {
        withEnv(
          for {
            svc <- ZIO.service[PresentationService]
            aRecord <- svc.createRecord()
            p = proposePresentation(aRecord.thid.value)
            aRecordReceived <- svc.receiveProposePresentation(p)

          } yield {
            assertTrue(aRecordReceived.id == aRecord.id)
            assertTrue(aRecordReceived.proposePresentationData == Some(p))
          }
        )
      },
      test("acceptProposePresentation updates the PresentatinRecord") {
        withEnv(
          for {
            svc <- ZIO.service[PresentationService]
            aRecord <- svc.createRecord()
            p = proposePresentation(aRecord.thid.value)
            aRecordReceived <- svc.receiveProposePresentation(p)
            repo <- ZIO.service[PresentationRepository[Task]]
            _ <- repo.updatePresentationRecordProtocolState(
              aRecord.id,
              PresentationRecord.ProtocolState.ProposalPending,
              PresentationRecord.ProtocolState.ProposalReceived
            )
            aRecordAccept <- svc.acceptProposePresentation(aRecord.id)

          } yield {
            assertTrue(aRecordReceived.id == aRecord.id)
            assertTrue(aRecordReceived.proposePresentationData == Some(p))
          }
        )
      },
    ).provideLayer(presentationServiceLayer)
  }

  def createIssuer(did: DID) = {
    val keyGen = KeyPairGenerator.getInstance("EC")
    keyGen.initialize(Curve.P_256.toECParameterSpec)
    val keyPair = keyGen.generateKeyPair()
    val privateKey = keyPair.getPrivate
    val publicKey = keyPair.getPublic
    Issuer(
      did = did,
      signer = ES256Signer(privateKey),
      publicKey = publicKey
    )
  }
  private def requestCredential = io.iohk.atala.mercury.protocol.issuecredential.RequestCredential(
    from = DidId("did:prism:aaa"),
    to = DidId("did:prism:bbb"),
    thid = Some(UUID.randomUUID.toString),
    body =
      io.iohk.atala.mercury.protocol.issuecredential.RequestCredential.Body(goal_code = Some("credential issuance")),
    attachments = Nil
  )

  private def requestPresentation: RequestPresentation = {
    val body = RequestPresentation.Body(goal_code = Some("Presentation Request"))
    val presentationAttachmentAsJson = """{
                "challenge": "1f44d55f-f161-4938-a659-f8026467f126",
                "domain": "us.gov/DriverLicense",
                "credential_manifest": {}
            }"""
    val prover = DidId("did:peer:Prover")
    val verifier = DidId("did:peer:Verifier")

    val attachmentDescriptor = AttachmentDescriptor.buildJsonAttachment(payload = presentationAttachmentAsJson)
    RequestPresentation(
      body = body,
      attachments = Seq(attachmentDescriptor),
      to = prover,
      from = verifier,
    )
  }

  private def proposePresentation(thid: String): ProposePresentation = {
    val presentationFormat = PresentationFormat(attach_id = "1", "format1")
    val body = ProposePresentation.Body(goal_code = Some("Propose Presentation"))
    val presentationAttachmentAsJson = """{
                "id": "1f44d55f-f161-4938-a659-f8026467f126",
                "subject": "subject",
                "credential_definition": {}
            }"""
    val attachmentDescriptor = AttachmentDescriptor.buildJsonAttachment(payload = presentationAttachmentAsJson)
    val prover = DidId("did:peer:Prover")
    val verifier = DidId("did:peer:Verifier")
    ProposePresentation(
      body = body,
      thid = Some(thid),
      attachments = Seq(attachmentDescriptor),
      to = verifier,
      from = prover
    )
  }
  private def presentation(thid: String): Presentation = {
    val presentationFormat = PresentationFormat(attach_id = "1", "format1")
    val body = Presentation.Body(goal_code = Some("Presentation"))
    val presentationAttachmentAsJson = """{
                "id": "1f44d55f-f161-4938-a659-f8026467f126",
                "subject": "subject",
                "credential_definition": {}
            }"""
    val attachmentDescriptor = AttachmentDescriptor.buildJsonAttachment(payload = presentationAttachmentAsJson)
    val prover = DidId("did:peer:Prover")
    val verifier = DidId("did:peer:Verifier")
    Presentation(
      body = body,
      thid = Some(thid),
      attachments = Seq(attachmentDescriptor),
      to = verifier,
      from = prover
    )
  }
  private def issueCredentialRecord = IssueCredentialRecord(
    id = DidCommID(),
    createdAt = Instant.ofEpochSecond(Instant.now.getEpochSecond()),
    updatedAt = None,
    thid = DidCommID(),
    schemaId = None,
    role = IssueCredentialRecord.Role.Issuer,
    subjectId = None,
    validityPeriod = None,
    automaticIssuance = None,
    awaitConfirmation = None,
    protocolState = IssueCredentialRecord.ProtocolState.OfferPending,
    publicationState = None,
    offerCredentialData = None,
    requestCredentialData = None,
    issueCredentialData = None,
    issuedCredentialRaw = None,
    issuingDID = None,
    metaRetries = 5,
    metaNextRetry = Some(Instant.now()),
    metaLastFailure = None,
  )

  extension (svc: PresentationService)
    def createRecord(
        pairwiseVerifierDID: DidId = DidId("did:prism:issuer"),
        pairwiseProverDID: DidId = DidId("did:prism:prover-pairwise"),
        thid: DidCommID = DidCommID(),
        schemaId: String = "schemaId",
        connectionId: Option[String] = None,
    ) = {
      val proofType = ProofType(schemaId, None, None)
      svc.createPresentationRecord(
        thid = thid,
        pairwiseVerifierDID = pairwiseVerifierDID,
        pairwiseProverDID = pairwiseProverDID,
        connectionId = Some("connectionId"),
        proofTypes = Seq(proofType),
        options = None,
      )
    }

}
