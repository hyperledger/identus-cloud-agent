package io.iohk.atala.pollux.core.service

import io.iohk.atala.event.notification.EventNotificationServiceError.EncoderError
import io.iohk.atala.event.notification.{Event, EventEncoder, EventNotificationService}
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.presentproof.{Presentation, ProofType, RequestPresentation}
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.presentation.Options
import io.iohk.atala.pollux.core.model.{DidCommID, PresentationRecord}
import io.iohk.atala.pollux.core.repository.{CredentialRepository, PresentationRepository}
import io.iohk.atala.pollux.core.service.PresentationServiceWithEventNotificationImpl.given
import zio.{IO, Task, URLayer, ZIO, ZLayer}

class PresentationServiceWithEventNotificationImpl(
    presentationRepository: PresentationRepository[Task],
    credentialRepository: CredentialRepository[Task],
    eventNotificationService: EventNotificationService
) extends PresentationServiceImpl(presentationRepository, credentialRepository) {
  override def createPresentationRecord(
      pairwiseVerifierDID: DidId,
      pairwiseProverDID: DidId,
      thid: DidCommID,
      connectionId: Option[String],
      proofTypes: Seq[ProofType],
      options: Option[Options]
  ): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(
      super.createPresentationRecord(pairwiseVerifierDID, pairwiseProverDID, thid, connectionId, proofTypes, options)
    )

  override def markRequestPresentationSent(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(super.markRequestPresentationSent(recordId))

  override def receiveRequestPresentation(
      connectionId: Option[_root_.java.lang.String],
      request: RequestPresentation
  ): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(super.receiveRequestPresentation(connectionId, request))

  override def markRequestPresentationRejected(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(super.markRequestPresentationRejected(recordId))

  override def acceptRequestPresentation(
      recordId: DidCommID,
      credentialsToUse: Seq[_root_.java.lang.String]
  ): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(super.acceptRequestPresentation(recordId, credentialsToUse))

  override def markPresentationGenerated(
      recordId: DidCommID,
      presentation: Presentation
  ): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(super.markPresentationGenerated(recordId, presentation))

  override def markPresentationSent(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(super.markPresentationSent(recordId))

  override def receivePresentation(presentation: Presentation): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(super.receivePresentation(presentation))

  override def markPresentationVerified(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(super.markPresentationVerified(recordId))

  override def markPresentationVerificationFailed(
      recordId: DidCommID
  ): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(super.markPresentationVerificationFailed(recordId))

  override def markPresentationAccepted(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(super.markPresentationAccepted(recordId))

  override def markPresentationRejected(recordId: DidCommID): IO[PresentationError, PresentationRecord] =
    notifyOnSuccess(super.markPresentationRejected(recordId))

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
}

object PresentationServiceWithEventNotificationImpl {

  given EventEncoder[PresentationRecord] = (data: PresentationRecord) =>
    ZIO.attempt(data.asInstanceOf[Any]).mapError(t => EncoderError(t.getMessage))

  val layer: URLayer[
    PresentationRepository[Task] & CredentialRepository[Task] & EventNotificationService,
    PresentationService
  ] =
    ZLayer.fromFunction(PresentationServiceWithEventNotificationImpl(_, _, _))

}
