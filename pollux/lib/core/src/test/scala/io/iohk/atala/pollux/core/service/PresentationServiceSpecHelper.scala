package io.iohk.atala.pollux.core.service

import com.nimbusds.jose.jwk.*
import io.iohk.atala.agent.walletapi.memory.GenericSecretStorageInMemory
import io.iohk.atala.mercury.model.{AttachmentDescriptor, DidId}
import io.iohk.atala.mercury.protocol.presentproof.*
import io.iohk.atala.mercury.{AgentPeerService, PeerDID}
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.repository.*
import io.iohk.atala.pollux.core.service.serdes.*
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import zio.*

import java.security.*
import java.time.Instant
import java.util.UUID

trait PresentationServiceSpecHelper {

  protected val defaultWalletLayer = ZLayer.succeed(WalletAccessContext(WalletId.default))

  val peerDidAgentLayer =
    AgentPeerService.makeLayer(PeerDID.makePeerDid(serviceEndpoint = Some("http://localhost:9099")))

  val genericSecretStorageLayer = GenericSecretStorageInMemory.layer
  val uriDereferencerLayer = ResourceURIDereferencerImpl.layer
  val credentialDefLayer =
    CredentialDefinitionRepositoryInMemory.layer ++ uriDereferencerLayer >>> CredentialDefinitionServiceImpl.layer
  val linkSecretLayer = genericSecretStorageLayer >+> LinkSecretServiceImpl.layer

  val presentationServiceLayer = ZLayer.make[
    PresentationService & CredentialDefinitionService & URIDereferencer & LinkSecretService & PresentationRepository &
      CredentialRepository
  ](
    PresentationServiceImpl.layer,
    credentialDefLayer,
    uriDereferencerLayer,
    linkSecretLayer,
    PresentationRepositoryInMemory.layer,
    CredentialRepositoryInMemory.layer
  ) ++ defaultWalletLayer

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

  protected def requestPresentation(credentialFormat: PresentCredentialRequestFormat): RequestPresentation = {
    val body = RequestPresentation.Body(goal_code = Some("Presentation Request"))
    val presentationAttachmentAsJson =
      """{
                "challenge": "1f44d55f-f161-4938-a659-f8026467f126",
                "domain": "us.gov/DriverLicense",
                "credential_manifest": {}
            }"""
    val prover = DidId("did:peer:Prover")
    val verifier = DidId("did:peer:Verifier")

    val attachmentDescriptor = AttachmentDescriptor.buildJsonAttachment(
      payload = presentationAttachmentAsJson,
      format = Some(credentialFormat.name)
    )
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

  protected def issueCredentialRecord(credentialFormat: CredentialFormat) = IssueCredentialRecord(
    id = DidCommID(),
    createdAt = Instant.now,
    updatedAt = None,
    thid = DidCommID(),
    schemaId = None,
    credentialDefinitionId = None,
    credentialFormat = credentialFormat,
    role = IssueCredentialRecord.Role.Issuer,
    subjectId = None,
    validityPeriod = None,
    automaticIssuance = None,
    protocolState = IssueCredentialRecord.ProtocolState.OfferPending,
    offerCredentialData = None,
    requestCredentialData = None,
    anonCredsRequestMetadata = None,
    issueCredentialData = None,
    issuedCredentialRaw = None,
    issuingDID = None,
    metaRetries = 5,
    metaNextRetry = Some(Instant.now()),
    metaLastFailure = None,
  )

  extension (svc: PresentationService)
    def createJwtRecord(
        pairwiseVerifierDID: DidId = DidId("did:prism:issuer"),
        pairwiseProverDID: DidId = DidId("did:prism:prover-pairwise"),
        thid: DidCommID = DidCommID(),
        schemaId: _root_.java.lang.String = "schemaId",
        options: Option[io.iohk.atala.pollux.core.model.presentation.Options] = None
    ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
      val proofType = ProofType(schemaId, None, None)
      svc.createJwtPresentationRecord(
        thid = thid,
        pairwiseVerifierDID = pairwiseVerifierDID,
        pairwiseProverDID = pairwiseProverDID,
        connectionId = Some("connectionId"),
        proofTypes = Seq(proofType),
        options = options
      )
    }

    def createAnoncredRecord(
        pairwiseVerifierDID: DidId = DidId("did:prism:issuer"),
        pairwiseProverDID: DidId = DidId("did:prism:prover-pairwise"),
        thid: DidCommID = DidCommID()
    ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
      val anoncredPresentationRequestV1 = AnoncredPresentationRequestV1(
        requested_attributes = Map(
          "sex" -> AnoncredRequestedAttributeV1(
            name = "sex",
            restrictions = List(
              AnoncredAttributeRestrictionV1(
                schema_id = None,
                cred_def_id = Some("$CRED_DEF_ID"),
                non_revoked = None
              )
            )
          )
        ),
        requested_predicates = Map(
          "age" -> AnoncredRequestedPredicateV1(
            name = "age",
            p_type = ">=",
            p_value = 18,
            restrictions = List.empty
          )
        ),
        name = "proof_req_1",
        nonce = "1103253414365527824079144",
        version = "0.1",
        non_revoked = Some(AnoncredNonRevokedIntervalV1(from = Some(1), to = Some(4)))
      )
      svc.createAnoncredPresentationRecord(
        thid = thid,
        pairwiseVerifierDID = pairwiseVerifierDID,
        pairwiseProverDID = pairwiseProverDID,
        connectionId = Some("connectionId"),
        anoncredPresentationRequestV1
      )
    }

    def createAnoncredRecordNoRestriction(
        pairwiseVerifierDID: DidId = DidId("did:prism:issuer"),
        pairwiseProverDID: DidId = DidId("did:prism:prover-pairwise"),
        thid: DidCommID = DidCommID()
    ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = {
      val anoncredPresentationRequestV1 = AnoncredPresentationRequestV1(
        requested_attributes = Map(
          "sex" -> AnoncredRequestedAttributeV1(
            name = "sex",
            restrictions = List.empty
          )
        ),
        requested_predicates = Map(
          "age" -> AnoncredRequestedPredicateV1(
            name = "age",
            p_type = ">=",
            p_value = 18,
            restrictions = List.empty
          )
        ),
        name = "proof_req_1",
        nonce = "1103253414365527824079144",
        version = "0.1",
        non_revoked = Some(AnoncredNonRevokedIntervalV1(from = Some(1), to = Some(4)))
      )
      svc.createAnoncredPresentationRecord(
        thid = thid,
        pairwiseVerifierDID = pairwiseVerifierDID,
        pairwiseProverDID = pairwiseProverDID,
        connectionId = Some("connectionId"),
        anoncredPresentationRequestV1
      )
    }
}
