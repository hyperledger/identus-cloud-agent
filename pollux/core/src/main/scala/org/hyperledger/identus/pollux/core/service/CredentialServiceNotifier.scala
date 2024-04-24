package org.hyperledger.identus.pollux.core.service

import io.circe.Json
import org.hyperledger.identus.castor.core.model.did.CanonicalPrismDID
import org.hyperledger.identus.event.notification.*
import org.hyperledger.identus.mercury.model.DidId
import org.hyperledger.identus.mercury.protocol.issuecredential.{IssueCredential, OfferCredential, RequestCredential}
import org.hyperledger.identus.pollux.core.model.error.CredentialServiceError
import org.hyperledger.identus.pollux.core.model.{DidCommID, IssueCredentialRecord}
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.{URLayer, ZIO, ZLayer, IO}

import java.util.UUID

class CredentialServiceNotifier(
    svc: CredentialService,
    eventNotificationService: EventNotificationService
) extends CredentialService {

  private val issueCredentialRecordUpdatedEvent = "IssueCredentialRecordUpdated"

  override def createJWTIssueCredentialRecord(
      pairwiseIssuerDID: DidId,
      pairwiseHolderDID: DidId,
      thid: DidCommID,
      maybeSchemaId: Option[String],
      claims: Json,
      validityPeriod: Option[Double],
      automaticIssuance: Option[Boolean],
      issuingDID: CanonicalPrismDID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(
      svc.createJWTIssueCredentialRecord(
        pairwiseIssuerDID,
        pairwiseHolderDID,
        thid,
        maybeSchemaId,
        claims,
        validityPeriod,
        automaticIssuance,
        issuingDID
      )
    )

  override def createAnonCredsIssueCredentialRecord(
      pairwiseIssuerDID: DidId,
      pairwiseHolderDID: DidId,
      thid: DidCommID,
      credentialDefinitionGUID: UUID,
      credentialDefinitionId: _root_.java.lang.String,
      claims: Json,
      validityPeriod: Option[Double],
      automaticIssuance: Option[Boolean]
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(
      svc.createAnonCredsIssueCredentialRecord(
        pairwiseIssuerDID,
        pairwiseHolderDID,
        thid,
        credentialDefinitionGUID,
        credentialDefinitionId,
        claims,
        validityPeriod,
        automaticIssuance
      )
    )

  override def markOfferSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.markOfferSent(recordId))

  override def receiveCredentialOffer(
      offer: OfferCredential
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.receiveCredentialOffer(offer))

  override def acceptCredentialOffer(
      recordId: DidCommID,
      subjectId: Option[String]
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.acceptCredentialOffer(recordId, subjectId))

  override def generateJWTCredentialRequest(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.generateJWTCredentialRequest(recordId))

  override def generateAnonCredsCredentialRequest(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.generateAnonCredsCredentialRequest(recordId))

  override def markRequestSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.markRequestSent(recordId))

  override def receiveCredentialRequest(
      request: RequestCredential
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.receiveCredentialRequest(request))

  override def acceptCredentialRequest(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.acceptCredentialRequest(recordId))

  override def markCredentialSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.markCredentialSent(recordId))

  override def receiveCredentialIssue(
      issueCredential: IssueCredential
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.receiveCredentialIssue(issueCredential))

  override def generateJWTCredential(
      recordId: DidCommID,
      statusListRegistryUrl: String
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.generateJWTCredential(recordId, statusListRegistryUrl))

  override def generateAnonCredsCredential(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, IssueCredentialRecord] =
    notifyOnSuccess(svc.generateAnonCredsCredential(recordId))

  private[this] def notifyOnSuccess[R](effect: ZIO[R, CredentialServiceError, IssueCredentialRecord]) =
    for {
      record <- effect
      _ <- notify(record)
    } yield record

  private[this] def notify(record: IssueCredentialRecord) = {
    val result = for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      producer <- eventNotificationService.producer[IssueCredentialRecord]("Issue")
      _ <- producer.send(Event(issueCredentialRecordUpdatedEvent, record, walletId))
    } yield ()
    result.catchAll(e => ZIO.logError(s"Notification service error: $e"))
  }

  override def reportProcessingFailure(
      recordId: DidCommID,
      failReason: Option[_root_.java.lang.String]
  ): ZIO[WalletAccessContext, CredentialServiceError, Unit] =
    svc.reportProcessingFailure(recordId, failReason)

  override def getIssueCredentialRecord(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, CredentialServiceError, Option[IssueCredentialRecord]] =
    svc.getIssueCredentialRecord(recordId)

  override def getIssueCredentialRecordByThreadId(
      thid: DidCommID,
      ignoreWithZeroRetries: Boolean
  ): ZIO[WalletAccessContext, CredentialServiceError, Option[IssueCredentialRecord]] =
    svc.getIssueCredentialRecordByThreadId(thid, ignoreWithZeroRetries)

  override def getIssueCredentialRecords(
      ignoreWithZeroRetries: Boolean,
      offset: Option[Int] = None,
      limit: Option[Int] = None
  ): ZIO[WalletAccessContext, CredentialServiceError, (Seq[IssueCredentialRecord], Int)] =
    svc.getIssueCredentialRecords(ignoreWithZeroRetries, offset, limit)

  override def getIssueCredentialRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: IssueCredentialRecord.ProtocolState*
  ): ZIO[WalletAccessContext, CredentialServiceError, Seq[IssueCredentialRecord]] =
    svc.getIssueCredentialRecordsByStates(ignoreWithZeroRetries, limit, states: _*)

  override def getIssueCredentialRecordsByStatesForAllWallets(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: IssueCredentialRecord.ProtocolState*
  ): IO[CredentialServiceError, Seq[IssueCredentialRecord]] =
    svc.getIssueCredentialRecordsByStatesForAllWallets(ignoreWithZeroRetries, limit, states: _*)
}

object CredentialServiceNotifier {
  val layer: URLayer[CredentialService & EventNotificationService, CredentialServiceNotifier] =
    ZLayer.fromFunction(CredentialServiceNotifier(_, _))
}
