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
import io.iohk.atala.shared.models.WalletAccessContext
import zio.{IO, ZIO}

import java.nio.charset.StandardCharsets
import java.time.Instant

trait CredentialService {

  def extractIdFromCredential(credential: W3cCredentialPayload): Option[DidCommID]

  def createIssueCredentialRecord(
      pairwiseIssuerDID: DidId,
      pairwiseHolderDID: DidId,
      thid: DidCommID,
      maybeSchemaId: Option[String],
      claims: io.circe.Json,
      validityPeriod: Option[Double] = None,
      automaticIssuance: Option[Boolean],
      awaitConfirmation: Option[Boolean],
      issuingDID: Option[CanonicalPrismDID]
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  /** Return a list of records as well as a count of all filtered items */
  def getIssueCredentialRecords(
      ignoreWithZeroRetries: Boolean,
      offset: Option[Int] = None,
      limit: Option[Int] = None
  ): ZIO[WalletAccessContext, CredentialServiceError, (Seq[IssueCredentialRecord], Int)]

  def getIssueCredentialRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: IssueCredentialRecord.ProtocolState*
  ): ZIO[WalletAccessContext, CredentialServiceError, Seq[IssueCredentialRecord]]

  def getIssueCredentialRecord(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, Option[IssueCredentialRecord]]

  def getIssueCredentialRecordByThreadId(
      thid: DidCommID,
      ignoreWithZeroRetries: Boolean
  ): ZIO[WalletAccessContext, CredentialServiceError, Option[IssueCredentialRecord]]

  def receiveCredentialOffer(
      offer: OfferCredential
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def acceptCredentialOffer(
      recordId: DidCommID,
      subjectId: String
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def createPresentationPayload(
      recordId: DidCommID,
      subject: Issuer
  ): ZIO[WalletAccessContext, CredentialServiceError, PresentationPayload]

  def generateCredentialRequest(
      recordId: DidCommID,
      signedPresentation: JWT
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def receiveCredentialRequest(
      request: RequestCredential
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def acceptCredentialRequest(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def createCredentialPayloadFromRecord(
      record: IssueCredentialRecord,
      issuer: Issuer,
      issuanceDate: Instant
  ): ZIO[WalletAccessContext, CredentialServiceError, W3cCredentialPayload]

  def publishCredentialBatch(
      credentials: Seq[W3cCredentialPayload],
      issuer: Issuer
  ): IO[CredentialServiceError, PublishedBatchData]

  def markCredentialRecordsAsPublishQueued(
      credentialsAndProofs: Seq[(W3cCredentialPayload, MerkleInclusionProof)]
  ): ZIO[WalletAccessContext, CredentialServiceError, Int]

  def receiveCredentialIssue(
      issue: IssueCredential
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def markOfferSent(recordId: DidCommID): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def markRequestSent(recordId: DidCommID): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def markCredentialGenerated(
      recordId: DidCommID,
      issueCredential: IssueCredential
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def markCredentialSent(recordId: DidCommID): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def markCredentialPublicationPending(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def markCredentialPublicationQueued(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def markCredentialPublished(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord]

  def reportProcessingFailure(
      recordId: DidCommID,
      failReason: Option[String]
  ): ZIO[WalletAccessContext, CredentialServiceError, Unit]

}

object CredentialService {
  def convertJsonClaimsToAttributes(
      claims: io.circe.Json
  ): IO[CredentialServiceError, Seq[Attribute]] = {
    for {
      fields <- ZIO.succeed(claims.asObject.map(_.toMap).getOrElse(Map.empty).toList)
      res <- ZIO.foreach(fields) {
        case (k, v) if v.isString =>
          ZIO.succeed(Attribute(k, v.asString.get, None))
        case (k, v) =>
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
        attr.mime_type match
          case None =>
            ZIO.succeed(jsonObject.add(attr.name, attr.value.asJson))

          case Some("application/json") =>
            val jsonBytes = java.util.Base64.getUrlDecoder.decode(attr.value.getBytes(StandardCharsets.UTF_8))
            io.circe.parser.parse(new String(jsonBytes, StandardCharsets.UTF_8)) match
              case Right(value) => ZIO.succeed(jsonObject.add(attr.name, value))
              case Left(error)  => ZIO.fail(UnsupportedVCClaimsValue(error.message))

          case Some(mimeType) =>
            ZIO.fail(UnsupportedVCClaimsMimeType(mimeType))
      }
    } yield claims
  }
}
