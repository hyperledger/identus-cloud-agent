package io.iohk.atala.pollux.core.service

import io.iohk.atala.mercury.model.*
import io.iohk.atala.mercury.protocol.presentproof.*
import io.iohk.atala.pollux.anoncreds.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.presentation.*
import io.iohk.atala.pollux.core.service.serdes.anoncreds.{CredentialProofsV1, PresentationRequestV1}
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.time.Instant
import java.util as ju
import java.util.UUID

trait PresentationService {
  def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID]

  def createJwtPresentationRecord(
      pairwiseVerifierDID: DidId,
      pairwiseProverDID: DidId,
      thid: DidCommID,
      connectionId: Option[String],
      proofTypes: Seq[ProofType],
      options: Option[io.iohk.atala.pollux.core.model.presentation.Options],
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def createAnoncredPresentationRecord(
      pairwiseVerifierDID: DidId,
      pairwiseProverDID: DidId,
      thid: DidCommID,
      connectionId: Option[String],
      presentationRequest: PresentationRequestV1
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def getPresentationRecords(
      ignoreWithZeroRetries: Boolean
  ): ZIO[WalletAccessContext, PresentationError, Seq[PresentationRecord]]

  def createJwtPresentationPayloadFromRecord(
      record: DidCommID,
      issuer: Issuer,
      issuanceDate: Instant
  ): ZIO[WalletAccessContext, PresentationError, PresentationPayload]

  def createAnoncredPresentationPayloadFromRecord(
      record: DidCommID,
      issuer: Issuer,
      anoncredCredentialProof: CredentialProofsV1,
      issuanceDate: Instant
  ): ZIO[WalletAccessContext, PresentationError, lib.Presentation]

  def createAnoncredPresentation(
      requestPresentation: RequestPresentation,
      recordId: DidCommID,
      prover: Issuer,
      anoncredCredentialProof: CredentialProofsV1,
      issuanceDate: Instant
  ): ZIO[WalletAccessContext, PresentationError, Presentation]

  def getPresentationRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      state: PresentationRecord.ProtocolState*
  ): ZIO[WalletAccessContext, PresentationError, Seq[PresentationRecord]]

  def getPresentationRecordsByStatesForAllWallets(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      state: PresentationRecord.ProtocolState*
  ): IO[PresentationError, Seq[PresentationRecord]]

  def getPresentationRecord(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, Option[PresentationRecord]]

  def getPresentationRecordByThreadId(
      thid: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, Option[PresentationRecord]]

  def receiveRequestPresentation(
      connectionId: Option[String],
      request: RequestPresentation
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def acceptRequestPresentation(
      recordId: DidCommID,
      credentialsToUse: Seq[String]
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def acceptAnoncredRequestPresentation(
      recordId: DidCommID,
      credentialsToUse: CredentialProofsV1
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def rejectRequestPresentation(recordId: DidCommID): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def receiveProposePresentation(
      request: ProposePresentation
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def acceptProposePresentation(recordId: DidCommID): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def receivePresentation(presentation: Presentation): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def acceptPresentation(recordId: DidCommID): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def rejectPresentation(recordId: DidCommID): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def markRequestPresentationSent(recordId: DidCommID): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def markRequestPresentationRejected(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def markProposePresentationSent(recordId: DidCommID): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def markPresentationSent(recordId: DidCommID): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def markPresentationGenerated(
      recordId: DidCommID,
      presentation: Presentation
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def markPresentationVerified(recordId: DidCommID): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def markPresentationRejected(recordId: DidCommID): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def markPresentationAccepted(recordId: DidCommID): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def markPresentationVerificationFailed(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def verifyAnoncredPresentation(
      presentation: Presentation,
      requestPresentation: RequestPresentation,
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord]

  def reportProcessingFailure(
      recordId: DidCommID,
      failReason: Option[String]
  ): ZIO[WalletAccessContext, PresentationError, Unit]

}
