package io.iohk.atala.pollux.core.service

import com.nimbusds.jose.jwk.*
import io.iohk.atala.mercury.model.{AttachmentDescriptor, DidId}
import io.iohk.atala.mercury.protocol.presentproof.*
import io.iohk.atala.mercury.{AgentPeerService, DidAgent, PeerDID}
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.repository.{CredentialRepository, CredentialRepositoryInMemory, PresentationRepositoryInMemory}
import io.iohk.atala.pollux.vc.jwt.*
import zio.*

import java.security.*
import java.time.Instant
import java.util.UUID

trait PresentationServiceSpecHelper {

  val peerDidAgentLayer =
    AgentPeerService.makeLayer(PeerDID.makePeerDid(serviceEndpoint = Some("http://localhost:9099")))
  val presentationServiceLayer =
    PresentationRepositoryInMemory.layer ++ CredentialRepositoryInMemory.layer ++ peerDidAgentLayer >>> PresentationServiceImpl.layer
  val presentationEnvLayer =
    PresentationRepositoryInMemory.layer ++ CredentialRepositoryInMemory.layer ++ presentationServiceLayer

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

  protected def requestCredential = io.iohk.atala.mercury.protocol.issuecredential.RequestCredential(
    from = DidId("did:prism:aaa"),
    to = DidId("did:prism:bbb"),
    thid = Some(UUID.randomUUID.toString),
    body =
      io.iohk.atala.mercury.protocol.issuecredential.RequestCredential.Body(goal_code = Some("credential issuance")),
    attachments = Nil
  )

  protected def requestPresentation: RequestPresentation = {
    val body = RequestPresentation.Body(goal_code = Some("Presentation Request"))
    val presentationAttachmentAsJson =
      """{
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

  protected def proposePresentation(thid: String): ProposePresentation = {
    val body = ProposePresentation.Body(goal_code = Some("Propose Presentation"))
    val presentationAttachmentAsJson =
      """{
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

  protected def presentation(thid: String): Presentation = {
    val body = Presentation.Body(goal_code = Some("Presentation"))
    val presentationAttachmentAsJson =
      """{
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

  protected def issueCredentialRecord = IssueCredentialRecord(
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
