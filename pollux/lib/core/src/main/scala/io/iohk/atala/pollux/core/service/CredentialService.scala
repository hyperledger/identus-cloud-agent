package io.iohk.atala.pollux.core.service

import io.circe.syntax.*
import io.circe.{Json, JsonObject}
import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.issuecredential.{Attribute, IssueCredential, OfferCredential, RequestCredential}
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.model.error.CredentialServiceError.*
import io.iohk.atala.pollux.vc.jwt.{Issuer, JWT, PresentationPayload, W3cCredentialPayload}
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import zio.{IO, ZIO}

import java.nio.charset.StandardCharsets
import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}
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

  def extractIdFromCredential(credential: W3cCredentialPayload): Option[DidCommID]

  def createIssueCredentialRecord(
      pairwiseIssuerDID: DidId,
      pairwiseHolderDID: DidId,
      thid: DidCommID,
      schemaId: Option[String],
      claims: io.circe.Json,
      validityPeriod: Option[Double] = None,
      automaticIssuance: Option[Boolean],
      awaitConfirmation: Option[Boolean],
      issuingDID: Option[CanonicalPrismDID]
  ): IO[CredentialServiceError, IssueCredentialRecord]

  /** Return the full list of CredentialRecords.
    *
    * TODO this function API maybe change in the future to return a lazy sequence of records or something similar to a
    * batabase cursor.
    */
  def getIssueCredentialRecords: IO[CredentialServiceError, Seq[IssueCredentialRecord]]

  def getIssueCredentialRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      states: IssueCredentialRecord.ProtocolState*
  ): IO[CredentialServiceError, Seq[IssueCredentialRecord]]

  /** Get the CredentialRecord by the record's id. If the record's id is not found the value None will be return
    * instead.
    */
  def getIssueCredentialRecord(recordId: DidCommID): IO[CredentialServiceError, Option[IssueCredentialRecord]]

  def receiveCredentialOffer(offer: OfferCredential): IO[CredentialServiceError, IssueCredentialRecord]

  def acceptCredentialOffer(recordId: DidCommID, subjectId: String): IO[CredentialServiceError, IssueCredentialRecord]

  def createPresentationPayload(recordId: DidCommID, subject: Issuer): IO[CredentialServiceError, PresentationPayload]

  def generateCredentialRequest(
      recordId: DidCommID,
      signedPresentation: JWT
  ): IO[CredentialServiceError, IssueCredentialRecord]

  def receiveCredentialRequest(request: RequestCredential): IO[CredentialServiceError, IssueCredentialRecord]

  def acceptCredentialRequest(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord]

  def createCredentialPayloadFromRecord(
      record: IssueCredentialRecord,
      issuer: Issuer,
      issuanceDate: Instant
  ): IO[CredentialServiceError, W3cCredentialPayload]

  def publishCredentialBatch(
      credentials: Seq[W3cCredentialPayload],
      issuer: Issuer
  ): IO[CredentialServiceError, PublishedBatchData]

  def markCredentialRecordsAsPublishQueued(
      credentialsAndProofs: Seq[(W3cCredentialPayload, MerkleInclusionProof)]
  ): IO[CredentialServiceError, Int]

  def receiveCredentialIssue(issue: IssueCredential): IO[CredentialServiceError, IssueCredentialRecord]

  def markOfferSent(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord]

  def markRequestSent(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord]

  def markCredentialGenerated(
      recordId: DidCommID,
      issueCredential: IssueCredential
  ): IO[CredentialServiceError, IssueCredentialRecord]

  def markCredentialSent(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord]

  def markCredentialPublicationPending(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord]

  def markCredentialPublicationQueued(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord]

  def markCredentialPublished(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord]

}

object CredentialService {
  def convertJsonClaimsToAttributes(
      claims: io.circe.Json
  ): IO[CredentialServiceError, Seq[Attribute]] = {
    for {
      fields <- ZIO.succeed(claims.asObject.map(_.toMap).getOrElse(Map.empty).toList)
      res <- ZIO.foreach(fields) { case (k, v) =>
        ZIO.succeed {
          val jsonValue = v.noSpaces
          Attribute(
            k,
            java.util.Base64.getUrlEncoder.encodeToString(jsonValue.getBytes(StandardCharsets.UTF_8)),
            Some("application/json")
          )
        }
      }
    } yield res
  }

  def convertAttributesToJsonClaims(
      attributes: Seq[Attribute]
  ): IO[CredentialServiceError, JsonObject] = {
    for {
      claims <- ZIO.foldLeft(attributes)(JsonObject()) { case (jsonObject, attr) =>
        attr.mimeType match
          case Some("application/json") =>
            val jsonBytes = java.util.Base64.getUrlDecoder.decode(attr.value.getBytes(StandardCharsets.UTF_8))
            io.circe.parser.parse(new String(jsonBytes, StandardCharsets.UTF_8)) match
              case Right(value) => ZIO.succeed(jsonObject.add(attr.name, value))
              case Left(error)  => ZIO.fail(UnsupportedVCClaimsValue(error.message))

          case Some(mimeType) =>
            ZIO.fail(UnsupportedVCClaimsMimeType(mimeType))

          case None =>
            ZIO.succeed(jsonObject.add(attr.name, attr.value.asJson))
      }
    } yield claims
  }
}
