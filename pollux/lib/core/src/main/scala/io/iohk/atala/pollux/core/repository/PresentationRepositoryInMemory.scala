package io.iohk.atala.pollux.core.repository

import io.iohk.atala.mercury.protocol.presentproof._
import io.iohk.atala.pollux.core.model._
import io.iohk.atala.pollux.core.model.PresentationRecord.ProtocolState
import io.iohk.atala.pollux.core.model.error.PresentationError._
import zio.*

import java.time.Instant
import java.util.UUID

class PresentationRepositoryInMemory(
    storeRef: Ref[Map[DidCommID, PresentationRecord]],
    maxRetries: Int
) extends PresentationRepository[Task] {

  override def createPresentationRecord(record: PresentationRecord): Task[Int] = {
    for {
      _ <- for {
        store <- storeRef.get
        maybeRecord = store.values.find(_.thid == record.thid)
      } yield ()
      _ <- storeRef.update(r => r + (record.id -> record))
    } yield 1
  }

  override def getPresentationRecord(recordId: DidCommID): Task[Option[PresentationRecord]] = {
    for {
      store <- storeRef.get
      record = store.get(recordId)
    } yield record
  }

  override def getPresentationRecords(
      ignoreWithZeroRetries: Boolean = true,
  ): Task[Seq[PresentationRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values.toSeq
  }

  override def updatePresentationRecordProtocolState(
      recordId: DidCommID,
      from: ProtocolState,
      to: ProtocolState
  ): Task[Int] = {
    for {
      store <- storeRef.get
      maybeRecord = store.find((id, record) => id == recordId && record.protocolState == from).map(_._2)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
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
  ): Task[Int] = {
    for {
      maybeRecord <- getPresentationRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
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
  ): Task[Int] = {
    for {
      maybeRecord <- getPresentationRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
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
  ): Task[Seq[PresentationRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values
      .filter(rec => states.contains(rec.protocolState) & (!ignoreWithZeroRetries | rec.metaRetries > 0))
      .take(limit)
      .toSeq
  }

  override def getPresentationRecordByThreadId(
      thid: DidCommID,
  ): Task[Option[PresentationRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values.find(_.thid == thid).filter(_.metaRetries > 0)
  }

  override def updateWithRequestPresentation(
      recordId: DidCommID,
      request: RequestPresentation,
      protocolState: ProtocolState
  ): Task[Int] = {
    for {
      maybeRecord <- getPresentationRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
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
  ): Task[Int] = {
    for {
      maybeRecord <- getPresentationRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
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
  ): Task[Int] = for {
    maybeRecord <- getPresentationRecord(recordId)
    count <- maybeRecord
      .map(record =>
        for {
          _ <- storeRef.update(r =>
            r.updated(
              recordId,
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
  val layer: ULayer[PresentationRepository[Task]] = ZLayer.fromZIO(
    Ref
      .make(Map.empty[DidCommID, PresentationRecord])
      .map(PresentationRepositoryInMemory(_, maxRetries))
  )
}
