package io.iohk.atala.pollux.core.service

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.mercury.protocol.issuecredential.OfferCredential
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.model.PublishedBatchData
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.model.error.CredentialServiceError._
import io.iohk.atala.pollux.vc.jwt.Issuer
import io.iohk.atala.pollux.vc.jwt.W3cCredentialPayload
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import zio.IO

import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.UUID

trait CredentialService {

  /** Copy pasted from Castor codebase for now TODO: replace with actual data from castor later
    *
    * @param method
    * @param methodSpecificId
    */
  final case class DID(
      method: String,
      methodSpecificId: String
  ) {
    override def toString: String = s"did:$method:$methodSpecificId"
  }

  // FIXME: this function is used as a temporary replacement
  // eventually, prism-agent should use castor library to get the issuer (issuance key and did)
  def createIssuer: Issuer = {
    val keyGen = KeyPairGenerator.getInstance("EC")
    val ecSpec = ECGenParameterSpec("secp256r1")
    keyGen.initialize(ecSpec, SecureRandom())
    val keyPair = keyGen.generateKeyPair()
    val privateKey = keyPair.getPrivate
    val publicKey = keyPair.getPublic
    val uuid = UUID.randomUUID().toString
    Issuer(
      did = io.iohk.atala.pollux.vc.jwt.DID(s"did:prism:$uuid"),
      signer = io.iohk.atala.pollux.vc.jwt.ES256Signer(privateKey),
      publicKey = publicKey
    )
  }

  def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID]

  def createIssueCredentialRecord(
      pairwiseDID: DidId,
      thid: UUID,
      subjectId: String,
      schemaId: Option[String],
      claims: Map[String, String],
      validityPeriod: Option[Double] = None,
      automaticIssuance: Option[Boolean],
      awaitConfirmation: Option[Boolean]
  ): IO[CredentialServiceError, IssueCredentialRecord]

  def getIssueCredentialRecords(): IO[CredentialServiceError, Seq[IssueCredentialRecord]]

  def getCredentialRecordsByState(
      state: IssueCredentialRecord.ProtocolState
  ): IO[CredentialServiceError, Seq[IssueCredentialRecord]]

  def getIssueCredentialRecord(recordId: UUID): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def receiveCredentialOffer(offer: OfferCredential): IO[CredentialServiceError, IssueCredentialRecord]

  def acceptCredentialOffer(recordId: UUID): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def receiveCredentialRequest(request: RequestCredential): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def acceptCredentialRequest(recordId: UUID): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def createCredentialPayloadFromRecord(
      record: IssueCredentialRecord,
      issuer: Issuer,
      issuanceDate: Instant
  ): IO[CreateCredentialPayloadFromRecordError, W3cCredentialPayload]

  def publishCredentialBatch(
      credentials: Seq[W3cCredentialPayload],
      issuer: Issuer
  ): IO[CredentialServiceError, PublishedBatchData]

  def markCredentialRecordsAsPublishQueued(
      credentialsAndProofs: Seq[(W3cCredentialPayload, MerkleInclusionProof)]
  ): IO[CredentialServiceError, Int]

  def receiveCredentialIssue(issue: IssueCredential): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def markOfferSent(recordId: UUID): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def markRequestSent(recordId: UUID): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def markCredentialGenerated(
      recordId: UUID,
      issueCredential: IssueCredential
  ): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def markCredentialSent(recordId: UUID): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def markCredentialPublicationPending(recordId: UUID): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def markCredentialPublicationQueued(recordId: UUID): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def markCredentialPublished(recordId: UUID): IO[CredentialServiceError, Option[IssueCredentialRecord]]

}
