package io.iohk.atala.pollux.core.repository

import io.iohk.atala.mercury.protocol.presentproof.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.PresentationRecord.ProtocolState
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

trait PresentationRepository {
  def createPresentationRecord(record: PresentationRecord): RIO[WalletAccessContext, Int]
  def getPresentationRecords(ignoreWithZeroRetries: Boolean): RIO[WalletAccessContext, Seq[PresentationRecord]]
  def getPresentationRecord(recordId: DidCommID): RIO[WalletAccessContext, Option[PresentationRecord]]
  def getPresentationRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: PresentationRecord.ProtocolState*
  ): RIO[WalletAccessContext, Seq[PresentationRecord]]
  def getPresentationRecordByThreadId(thid: DidCommID): RIO[WalletAccessContext, Option[PresentationRecord]]

  def updatePresentationRecordProtocolState(
      recordId: DidCommID,
      from: PresentationRecord.ProtocolState,
      to: PresentationRecord.ProtocolState
  ): RIO[WalletAccessContext, Int]

  def updateWithRequestPresentation(
      recordId: DidCommID,
      request: RequestPresentation,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int]
  def updateWithProposePresentation(
      recordId: DidCommID,
      request: ProposePresentation,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int]
  def updateWithPresentation(
      recordId: DidCommID,
      presentation: Presentation,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int]
  def updatePresentationWithCredentialsToUse(
      recordId: DidCommID,
      credentialsToUse: Option[Seq[String]],
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int]

  def updateAfterFail(
      recordId: DidCommID,
      failReason: Option[String]
  ): RIO[WalletAccessContext, Int]
}
