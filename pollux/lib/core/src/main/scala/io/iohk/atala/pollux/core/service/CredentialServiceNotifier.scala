package io.iohk.atala.pollux.core.service

import io.circe.Json
import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.event.notification.*
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.issuecredential.{IssueCredential, OfferCredential, RequestCredential}
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.model.{DidCommID, IssueCredentialRecord, PublishedBatchData}
import io.iohk.atala.pollux.vc.jwt.{Issuer, JWT, PresentationPayload, W3cCredentialPayload}
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import zio.{IO, URLayer, ZIO, ZLayer}

import java.time.Instant

class CredentialServiceNotifier(
    svc: CredentialService,
    eventNotificationService: EventNotificationService
) extends CredentialService {

  override def createIssueCredentialRecord(
      pairwiseIssuerDID: DidId,
      pairwiseHolderDID: DidId,
      thid: DidCommID,
      maybeSchemaId: Option[_root_.java.lang.String],
      claims: Json,
      validityPeriod: Option[Double],
      automaticIssuance: Option[Boolean],
      awaitConfirmation: Option[Boolean],
      issuingDID: Option[CanonicalPrismDID]
  ): IO[CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(
      svc.createIssueCredentialRecord(
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
    )

  override def markOfferSent(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.markOfferSent(recordId))

  override def receiveCredentialOffer(offer: OfferCredential): IO[CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.receiveCredentialOffer(offer))

  override def acceptCredentialOffer(
      recordId: DidCommID,
      subjectId: String
  ): IO[CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.acceptCredentialOffer(recordId, subjectId))

  override def generateCredentialRequest(
      recordId: DidCommID,
      signedPresentation: JWT
  ): IO[CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.generateCredentialRequest(recordId, signedPresentation))

  override def markRequestSent(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.markRequestSent(recordId))

  override def receiveCredentialRequest(request: RequestCredential): IO[CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.receiveCredentialRequest(request))

  override def acceptCredentialRequest(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.acceptCredentialRequest(recordId))

  override def markCredentialGenerated(
      recordId: DidCommID,
      issueCredential: IssueCredential
  ): IO[CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.markCredentialGenerated(recordId, issueCredential))

  override def markCredentialSent(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.markCredentialSent(recordId))

  override def receiveCredentialIssue(issue: IssueCredential): IO[CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.receiveCredentialIssue(issue))

  private[this] def notifyOnSuccess(effect: IO[CredentialServiceError, IssueCredentialRecord]) =
    for {
      record <- effect
      _ <- notify(record)
    } yield record

  private[this] def notify(record: IssueCredentialRecord) = {
    val result = for {
      producer <- eventNotificationService.producer[IssueCredentialRecord]("Issue")
      _ <- producer.send(Event(record))
    } yield ()
    result.catchAll(e => ZIO.logError(s"Notification service error: $e"))
  }

  override def createPresentationPayload(
      recordId: DidCommID,
      subject: Issuer
  ): IO[CredentialServiceError, PresentationPayload] =
    svc.createPresentationPayload(recordId, subject)

  override def createCredentialPayloadFromRecord(
      record: IssueCredentialRecord,
      issuer: Issuer,
      issuanceDate: Instant
  ): IO[CredentialServiceError, W3cCredentialPayload] =
    svc.createCredentialPayloadFromRecord(record, issuer, issuanceDate)

  override def publishCredentialBatch(
      credentials: Seq[W3cCredentialPayload],
      issuer: Issuer
  ): IO[CredentialServiceError, PublishedBatchData] =
    svc.publishCredentialBatch(credentials, issuer)

  override def markCredentialRecordsAsPublishQueued(
      credentialsAndProofs: Seq[(W3cCredentialPayload, MerkleInclusionProof)]
  ): IO[CredentialServiceError, Int] =
    svc.markCredentialRecordsAsPublishQueued(credentialsAndProofs)

  override def markCredentialPublicationPending(
      recordId: DidCommID
  ): IO[CredentialServiceError, IssueCredentialRecord] =
    svc.markCredentialPublicationPending(recordId)

  override def markCredentialPublicationQueued(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord] =
    svc.markCredentialPublicationQueued(recordId)

  override def markCredentialPublished(recordId: DidCommID): IO[CredentialServiceError, IssueCredentialRecord] =
    svc.markCredentialPublished(recordId)

  override def reportProcessingFailure(
      recordId: DidCommID,
      failReason: Option[_root_.java.lang.String]
  ): IO[CredentialServiceError, Unit] =
    svc.reportProcessingFailure(recordId, failReason)

  override def extractIdFromCredential(credential: W3cCredentialPayload): Option[DidCommID] =
    svc.extractIdFromCredential(credential)

  override def getIssueCredentialRecord(
      recordId: DidCommID
  ): IO[CredentialServiceError, Option[IssueCredentialRecord]] =
    svc.getIssueCredentialRecord(recordId)

  override def getIssueCredentialRecords: IO[CredentialServiceError, Seq[IssueCredentialRecord]] =
    svc.getIssueCredentialRecords

  override def getIssueCredentialRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: IssueCredentialRecord.ProtocolState*
  ): IO[CredentialServiceError, Seq[IssueCredentialRecord]] =
    svc.getIssueCredentialRecordsByStates(ignoreWithZeroRetries, limit, states: _*)
}

object CredentialServiceNotifier {
  val layer: URLayer[CredentialService & EventNotificationService, CredentialServiceNotifier] =
    ZLayer.fromFunction(CredentialServiceNotifier(_, _))
}
