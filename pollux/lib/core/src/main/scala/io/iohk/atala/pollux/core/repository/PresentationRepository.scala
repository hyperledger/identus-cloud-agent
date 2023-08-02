package io.iohk.atala.pollux.core.repository

import io.iohk.atala.mercury.protocol.presentproof.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.PresentationRecord.ProtocolState

trait PresentationRepository[F[_]] {
  def createPresentationRecord(record: PresentationRecord): F[Int]
  def getPresentationRecords(ignoreWithZeroRetries: Boolean = true): F[Seq[PresentationRecord]]
  def getPresentationRecord(recordId: DidCommID): F[Option[PresentationRecord]]
  def getPresentationRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: PresentationRecord.ProtocolState*
  ): F[Seq[PresentationRecord]]
  def getPresentationRecordByThreadId(thid: DidCommID): F[Option[PresentationRecord]]

  def updatePresentationRecordProtocolState(
      recordId: DidCommID,
      from: PresentationRecord.ProtocolState,
      to: PresentationRecord.ProtocolState
  ): F[Int]

  def updateWithRequestPresentation(
      recordId: DidCommID,
      request: RequestPresentation,
      protocolState: ProtocolState
  ): F[Int]
  def updateWithProposePresentation(
      recordId: DidCommID,
      request: ProposePresentation,
      protocolState: ProtocolState
  ): F[Int]
  def updateWithPresentation(
      recordId: DidCommID,
      presentation: Presentation,
      protocolState: ProtocolState
  ): F[Int]
  def updatePresentationWithCredentialsToUse(
      recordId: DidCommID,
      credentialsToUse: Option[Seq[String]],
      protocolState: ProtocolState
  ): F[Int]

  def updateAfterFail(
      recordId: DidCommID,
      failReason: Option[String]
  ): F[Int]
}
