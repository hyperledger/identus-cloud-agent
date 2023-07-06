package io.iohk.atala.pollux.core.service
import io.iohk.atala.event.notification.{Event, EventNotificationService}
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.presentproof.{Presentation, ProofType, ProposePresentation, RequestPresentation}
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.presentation.Options
import io.iohk.atala.pollux.core.model.{DidCommID, PresentationRecord}
import io.iohk.atala.pollux.vc.jwt.{Issuer, PresentationPayload, W3cCredentialPayload}
import zio.{IO, URLayer, ZIO, ZLayer}

import java.time.Instant
import java.util.UUID

class PresentationServiceNotifier(
    svc: PresentationService,
    eventNotificationService: EventNotificationService
) extends PresentationService {

  override def createPresentationRecord(
      pairwiseVerifierDID: DidId,
      pairwiseProverDID: DidId,
      thid: DidCommID,
      connectionId: Option[String],
      proofTypes: Seq[ProofType],
      options: Option[Options]
  ): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(
      svc.createPresentationRecord(pairwiseVerifierDID, pairwiseProverDID, thid, connectionId, proofTypes, options)
    )

  override def markRequestPresentationSent(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markRequestPresentationSent(recordId))

  override def receiveRequestPresentation(
      connectionId: Option[String],
      request: RequestPresentation
  ): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.receiveRequestPresentation(connectionId, request))

  override def markRequestPresentationRejected(
      recordId: DidCommID
  ): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markRequestPresentationRejected(recordId))

  override def acceptRequestPresentation(
      recordId: DidCommID,
      credentialsToUse: Seq[String]
  ): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.acceptRequestPresentation(recordId, credentialsToUse))

  override def rejectRequestPresentation(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.rejectRequestPresentation(recordId))

  override def markPresentationGenerated(
      recordId: DidCommID,
      presentation: Presentation
  ): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markPresentationGenerated(recordId, presentation))

  override def markPresentationSent(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markPresentationSent(recordId))

  override def receivePresentation(presentation: Presentation): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.receivePresentation(presentation))

  override def markPresentationVerified(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markPresentationVerified(recordId))

  override def markPresentationVerificationFailed(
      recordId: DidCommID
  ): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markPresentationVerificationFailed(recordId))

  override def acceptPresentation(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.acceptPresentation(recordId))

  override def rejectPresentation(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.rejectPresentation(recordId))

  private[this] def notifyOnSuccess(effect: IO[PresentationError, PresentationRecord]) =
    for {
      record <- effect
      _ <- notify(record)
    } yield record

  private[this] def notify(record: PresentationRecord) = {
    val result = for {
      producer <- eventNotificationService.producer[PresentationRecord]("Presentation")
      _ <- producer.send(Event(record))
    } yield ()
    result.catchAll(e => ZIO.logError(s"Notification service error: $e"))
  }

  override def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID] =
    svc.extractIdFromCredential(credential)

  override def getPresentationRecords(): IO[PresentationError, Seq[PresentationRecord]] = svc.getPresentationRecords()

  override def createPresentationPayloadFromRecord(
      record: DidCommID,
      issuer: Issuer,
      issuanceDate: Instant
  ): IO[PresentationError, PresentationPayload] = svc.createPresentationPayloadFromRecord(record, issuer, issuanceDate)

  override def getPresentationRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      state: PresentationRecord.ProtocolState*
  ): IO[PresentationError, Seq[PresentationRecord]] =
    svc.getPresentationRecordsByStates(ignoreWithZeroRetries, limit, state: _*)

  override def getPresentationRecord(recordId: DidCommID): IO[PresentationError, Option[PresentationRecord]] =
    svc.getPresentationRecord(recordId)

  override def getPresentationRecordByThreadId(thid: DidCommID): IO[PresentationError, Option[PresentationRecord]] =
    svc.getPresentationRecordByThreadId(thid)

  override def receiveProposePresentation(request: ProposePresentation): IO[PresentationError, PresentationRecord] =
    svc.receiveProposePresentation((request))

  override def acceptProposePresentation(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    svc.acceptPresentation(recordId)

  override def markProposePresentationSent(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    svc.markProposePresentationSent(recordId)

  override def markPresentationAccepted(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    svc.markPresentationAccepted(recordId)

  override def markPresentationRejected(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    svc.markPresentationRejected(recordId)

  override def reportProcessingFailure(
      recordId: DidCommID,
      failReason: Option[_root_.java.lang.String]
  ): IO[PresentationError, Unit] = svc.reportProcessingFailure(recordId, failReason)
}

object PresentationServiceNotifier {
  val layer: URLayer[EventNotificationService & PresentationService, PresentationService] =
    ZLayer.fromFunction(PresentationServiceNotifier(_, _))
}
