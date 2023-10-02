package io.iohk.atala.pollux.core.service

import io.circe.Json
import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.issuecredential.{IssueCredential, OfferCredential, RequestCredential}
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.model.{DidCommID, IssueCredentialRecord, PublishedBatchData}
import io.iohk.atala.pollux.vc.jwt.{Issuer, JWT, PresentationPayload, W3cCredentialPayload}
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import zio.mock.{Mock, Proxy}
import zio.{IO, URLayer, ZIO, ZLayer, mock}

import java.time.Instant

object MockCredentialService extends Mock[CredentialService] {

  object CreateIssueCredentialRecord
      extends Effect[
        (
            DidId,
            DidId,
            DidCommID,
            Option[String],
            Json,
            Option[Double],
            Option[Boolean],
            Option[Boolean],
            Option[CanonicalPrismDID]
        ),
        CredentialServiceError,
        IssueCredentialRecord
      ]

  object ReceiveCredentialOffer extends Effect[OfferCredential, CredentialServiceError, IssueCredentialRecord]
  object AcceptCredentialOffer extends Effect[(DidCommID, String), CredentialServiceError, IssueCredentialRecord]
  object CreatePresentationPayload extends Effect[(DidCommID, Issuer), CredentialServiceError, PresentationPayload]
  object GenerateCredentialRequest extends Effect[(DidCommID, JWT), CredentialServiceError, IssueCredentialRecord]
  object ReceiveCredentialRequest extends Effect[RequestCredential, CredentialServiceError, IssueCredentialRecord]
  object AcceptCredentialRequest extends Effect[DidCommID, CredentialServiceError, IssueCredentialRecord]
  object CreateCredentialPayloadFromRecord
      extends Effect[(IssueCredentialRecord, Issuer, Instant), CredentialServiceError, W3cCredentialPayload]
  object PublishCredentialBatch
      extends Effect[(Seq[W3cCredentialPayload], Issuer), CredentialServiceError, PublishedBatchData]
  object MarkCredentialRecordsAsPublishQueued
      extends Effect[Seq[(W3cCredentialPayload, MerkleInclusionProof)], CredentialServiceError, Int]
  object ReceiveCredentialIssue extends Effect[IssueCredential, CredentialServiceError, IssueCredentialRecord]
  object MarkOfferSent extends Effect[DidCommID, CredentialServiceError, IssueCredentialRecord]
  object MarkRequestSent extends Effect[DidCommID, CredentialServiceError, IssueCredentialRecord]
  object MarkCredentialGenerated
      extends Effect[(DidCommID, IssueCredential), CredentialServiceError, IssueCredentialRecord]
  object MarkCredentialSent extends Effect[DidCommID, CredentialServiceError, IssueCredentialRecord]
  object MarkCredentialPublicationPending extends Effect[DidCommID, CredentialServiceError, IssueCredentialRecord]
  object MarkCredentialPublicationQueued extends Effect[DidCommID, CredentialServiceError, IssueCredentialRecord]
  object MarkCredentialPublished extends Effect[DidCommID, CredentialServiceError, IssueCredentialRecord]
  object ReportProcessingFailure extends Effect[(DidCommID, Option[String]), CredentialServiceError, Unit]

  override val compose: URLayer[mock.Proxy, CredentialService] = ZLayer {
    for {
      proxy <- ZIO.service[Proxy]
    } yield new CredentialService {

      override def createIssueCredentialRecord(
          pairwiseIssuerDID: DidId,
          pairwiseHolderDID: DidId,
          thid: DidCommID,
          maybeSchemaId: Option[String],
          claims: Json,
          validityPeriod: Option[Double],
          automaticIssuance: Option[Boolean],
          awaitConfirmation: Option[Boolean],
          issuingDID: Option[CanonicalPrismDID]
      ): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(
          CreateIssueCredentialRecord,
          pairwiseIssuerDID,
          pairwiseHolderDID,
          thid,
          maybeSchemaId,
          claims,
          validityPeriod,
          automaticIssuance,
          awaitConfirmation,
          issuingDID
        )

      override def receiveCredentialOffer(offer: OfferCredential): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(ReceiveCredentialOffer, offer)

      override def acceptCredentialOffer(
          recordId: DidCommID,
          subjectId: String
      ): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(AcceptCredentialOffer, recordId, subjectId)

      override def createPresentationPayload(
          recordId: DidCommID,
          subject: Issuer
      ): IO[CredentialServiceError, PresentationPayload] =
        proxy(CreatePresentationPayload, recordId, subject)

      override def generateCredentialRequest(
          recordId: DidCommID,
          signedPresentation: JWT
      ): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(GenerateCredentialRequest, recordId, signedPresentation)

      override def receiveCredentialRequest(
          request: RequestCredential
      ): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(ReceiveCredentialRequest, request)

      override def acceptCredentialRequest(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(AcceptCredentialRequest, recordId)

      override def createCredentialPayloadFromRecord(
          record: IssueCredentialRecord,
          issuer: Issuer,
          issuanceDate: Instant
      ): IO[CredentialServiceError, W3cCredentialPayload] =
        proxy(CreateCredentialPayloadFromRecord, record, issuer, issuanceDate)

      override def publishCredentialBatch(
          credentials: Seq[W3cCredentialPayload],
          issuer: Issuer
      ): IO[CredentialServiceError, PublishedBatchData] =
        proxy(PublishCredentialBatch, credentials, issuer)

      override def markCredentialRecordsAsPublishQueued(
          credentialsAndProofs: Seq[(W3cCredentialPayload, MerkleInclusionProof)]
      ): IO[CredentialServiceError, Int] =
        proxy(MarkCredentialRecordsAsPublishQueued, credentialsAndProofs)

      override def receiveCredentialIssue(issue: IssueCredential): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(ReceiveCredentialIssue, issue)

      override def markOfferSent(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(MarkOfferSent, recordId)

      override def markRequestSent(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(MarkRequestSent, recordId)

      override def markCredentialGenerated(
          recordId: DidCommID,
          issueCredential: IssueCredential
      ): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(MarkCredentialGenerated, recordId, issueCredential)

      override def markCredentialSent(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(MarkCredentialSent, recordId)

      override def markCredentialPublicationPending(
          recordId: DidCommID
      ): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(MarkCredentialPublicationPending, recordId)

      override def markCredentialPublicationQueued(
          recordId: DidCommID
      ): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(MarkCredentialPublicationQueued, recordId)

      override def markCredentialPublished(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord] =
        proxy(MarkCredentialPublished, recordId)

      override def reportProcessingFailure(
          recordId: DidCommID,
          failReason: Option[String]
      ): IO[CredentialServiceError, Unit] =
        proxy(ReportProcessingFailure, recordId, failReason)

      override def extractIdFromCredential(credential: W3cCredentialPayload): Option[DidCommID] =
        ???

      override def getIssueCredentialRecords(
          ignoreWithZeroRetries: Boolean,
          offset: Option[Int] = None,
          limit: Option[Int] = None
      ): IO[CredentialServiceError, (Seq[IssueCredentialRecord], Int)] =
        ???

      override def getIssueCredentialRecordsByStates(
          ignoreWithZeroRetries: Boolean,
          limit: Int,
          states: IssueCredentialRecord.ProtocolState*
      ): IO[CredentialServiceError, Seq[IssueCredentialRecord]] =
        ???

      override def getIssueCredentialRecord(
          recordId: DidCommID
      ): IO[CredentialServiceError, Option[IssueCredentialRecord]] =
        ???

      override def getIssueCredentialRecordByThreadId(
          thid: DidCommID,
          ignoreWithZeroRetries: Boolean
      ): IO[CredentialServiceError, Option[IssueCredentialRecord]] = ???
    }
  }
}
