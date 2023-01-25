package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.EncodedJWTCredential
import io.iohk.atala.pollux.core.model.PresentationRecord
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import zio.*

import java.util.UUID
import io.iohk.atala.mercury.protocol.presentproof._
import io.iohk.atala.pollux.core.model.PresentationRecord.ProtocolState

trait PresentationRepository[F[_]] {
  def createPresentationRecord(record: PresentationRecord): F[Int]
  def getPresentationRecords(): F[Seq[PresentationRecord]]
  def getPresentationRecord(recordId: UUID): F[Option[PresentationRecord]]
  def getPresentationRecordsByState(state: PresentationRecord.ProtocolState): F[Seq[PresentationRecord]]
  def getPresentationRecordByThreadId(thid: UUID): F[Option[PresentationRecord]]

  def updatePresentationRecordProtocolState(
      recordId: UUID,
      from: PresentationRecord.ProtocolState,
      to: PresentationRecord.ProtocolState
  ): F[Int]

  def updateWithRequestPresentation(recordId: UUID, request: RequestPresentation, protocolState: ProtocolState): F[Int]
  def updateWithProposePresentation(recordId: UUID, request: ProposePresentation, protocolState: ProtocolState): F[Int]
  def updateWithPresentation(recordId: UUID, presentation: Presentation, protocolState: ProtocolState): F[Int]
  def updatePresentationWithCredentialsToUse(
      recordId: UUID,
      credentialsToUse: Option[Seq[String]],
      protocolState: ProtocolState
  ): F[Int]

}
