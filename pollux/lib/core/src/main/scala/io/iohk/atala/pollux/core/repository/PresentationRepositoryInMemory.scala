package io.iohk.atala.pollux.core.repository

import io.iohk.atala.mercury.protocol.presentproof._
import io.iohk.atala.pollux.core.model.PresentationRecord.ProtocolState
import io.iohk.atala.pollux.core.model._
import io.iohk.atala.pollux.core.model.error.PresentationError._
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

import java.time.Instant

class PresentationRepositoryInMemory(
    storeRef: Ref[Map[(WalletId, DidCommID), PresentationRecord]],
    maxRetries: Int
) extends PresentationRepository {

  override def createPresentationRecord(record: PresentationRecord): RIO[WalletAccessContext, Int] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      _ <- for {
        store <- storeRef.get
        maybeRecord = store.values.find(_.thid == record.thid)
      } yield ()
      _ <- storeRef.update(r => r + ((walletId, record.id) -> record))
    } yield 1
  }

  override def getPresentationRecord(recordId: DidCommID): RIO[WalletAccessContext, Option[PresentationRecord]] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      store <- storeRef.get
      record = store.get((walletId, recordId))
    } yield record
  }

  override def getPresentationRecords(
      ignoreWithZeroRetries: Boolean = true,
  ): RIO[WalletAccessContext, Seq[PresentationRecord]] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      store <- storeRef.get.map(_.filter { case ((wid, _), _) => walletId == wid })
    } yield store.values.toSeq
  }

  override def updatePresentationRecordProtocolState(
      recordId: DidCommID,
      from: ProtocolState,
      to: ProtocolState
  ): RIO[WalletAccessContext, Int] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      store <- storeRef.get
      maybeRecord = store.find((id, record) => id == (walletId, recordId) && record.protocolState == from).map(_._2)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                (walletId, recordId),
                record.copy(
                  protocolState = to,
                  metaRetries = maxRetries,
                  metaLastFailure = None,
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def updatePresentationWithCredentialsToUse(
      recordId: DidCommID,
      credentialsToUse: Option[Seq[String]],
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      maybeRecord <- getPresentationRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                (walletId, recordId),
                record.copy(
                  updatedAt = Some(Instant.now),
                  credentialsToUse = credentialsToUse.map(_.toList),
                  protocolState = protocolState,
                  metaRetries = maxRetries,
                  metaLastFailure = None,
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def updateWithPresentation(
      recordId: DidCommID,
      presentation: Presentation,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      maybeRecord <- getPresentationRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                (walletId, recordId),
                record.copy(
                  updatedAt = Some(Instant.now),
                  presentationData = Some(presentation),
                  protocolState = protocolState,
                  metaRetries = maxRetries,
                  metaLastFailure = None,
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def getPresentationRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: ProtocolState*
  ): RIO[WalletAccessContext, Seq[PresentationRecord]] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      store <- storeRef.get.map(_.filter { case ((wid, _), _) => walletId == wid })
    } yield store.values
      .filter(rec => states.contains(rec.protocolState) & (!ignoreWithZeroRetries | rec.metaRetries > 0))
      .take(limit)
      .toSeq
  }

  override def getPresentationRecordByThreadId(
      thid: DidCommID,
  ): RIO[WalletAccessContext, Option[PresentationRecord]] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      store <- storeRef.get.map(_.filter { case ((wid, _), _) => walletId == wid })
    } yield store.values.find(_.thid == thid).filter(_.metaRetries > 0)
  }

  override def updateWithRequestPresentation(
      recordId: DidCommID,
      request: RequestPresentation,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      maybeRecord <- getPresentationRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                (walletId, recordId),
                record.copy(
                  updatedAt = Some(Instant.now),
                  requestPresentationData = Some(request),
                  protocolState = protocolState,
                  metaRetries = maxRetries,
                  metaLastFailure = None,
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }
  override def updateWithProposePresentation(
      recordId: DidCommID,
      request: ProposePresentation,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      maybeRecord <- getPresentationRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                (walletId, recordId),
                record.copy(
                  updatedAt = Some(Instant.now),
                  proposePresentationData = Some(request),
                  protocolState = protocolState,
                  metaRetries = maxRetries,
                  metaLastFailure = None,
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  def updateAfterFail(
      recordId: DidCommID,
      failReason: Option[String]
  ): RIO[WalletAccessContext, Int] = for {
    maybeRecord <- getPresentationRecord(recordId)
    count <- maybeRecord
      .map(record =>
        for {
          walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
          _ <- storeRef.update(r =>
            r.updated(
              (walletId, recordId),
              record.copy(
                metaRetries = math.max(0, record.metaRetries - 1),
                metaNextRetry =
                  if (record.metaRetries - 1 <= 0) None
                  else Some(Instant.now().plusSeconds(60)), // TODO exponention time
                metaLastFailure = failReason
              )
            )
          )
        } yield 1
      )
      .getOrElse(ZIO.succeed(0))
  } yield count

}

object PresentationRepositoryInMemory {
  val maxRetries = 5 // TODO Move to config
  val layer: ULayer[PresentationRepository] = ZLayer.fromZIO(
    Ref
      .make(Map.empty[(WalletId, DidCommID), PresentationRecord])
      .map(PresentationRepositoryInMemory(_, maxRetries))
  )
}
