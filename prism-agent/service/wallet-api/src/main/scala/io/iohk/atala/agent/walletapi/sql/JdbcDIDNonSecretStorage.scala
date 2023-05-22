package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.iohk.atala.agent.walletapi.model.{DIDUpdateLineage, ManagedDIDState, ManagedDIDStatePatch}
import io.iohk.atala.agent.walletapi.storage.DIDNonSecretStorage
import io.iohk.atala.castor.core.model.did.{PrismDID, ScheduledDIDOperationStatus}
import zio.*
import zio.interop.catz.*

import java.time.Instant
import io.iohk.atala.agent.walletapi.model.ManagedDIDHdKeyPath
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.castor.core.model.did.InternalKeyPurpose
import io.iohk.atala.agent.walletapi.model.PublicationState
import io.iohk.atala.agent.walletapi.model.HdKeyIndexCounter
import io.iohk.atala.agent.walletapi.model.InternalKeyCounter
import io.iohk.atala.agent.walletapi.model.VerificationRelationshipCounter

class JdbcDIDNonSecretStorage(xa: Transactor[Task]) extends DIDNonSecretStorage {

  override def getManagedDIDState(did: PrismDID): Task[Option[ManagedDIDState]] = {
    val cxnIO =
      sql"""
        | SELECT
        |   did,
        |   publication_status,
        |   atala_operation_content,
        |   publish_operation_id,
        |   created_at,
        |   updated_at,
        |   key_mode,
        |   did_index
        | FROM public.prism_did_wallet_state
        | WHERE did = $did
        """.stripMargin
        .query[DIDStateRow]
        .option

    cxnIO
      .transact(xa)
      .flatMap(_.map(_.toDomain).fold(ZIO.none)(t => ZIO.fromTry(t).asSome))
  }

  override def insertManagedDID(
      did: PrismDID,
      state: ManagedDIDState,
      hdKey: Map[String, ManagedDIDHdKeyPath]
  ): Task[Unit] = {
    val insertStateIO = (row: DIDStateRow) => sql"""
        | INSERT INTO public.prism_did_wallet_state(
        |   did,
        |   publication_status,
        |   atala_operation_content,
        |   publish_operation_id,
        |   created_at,
        |   updated_at,
        |   key_mode,
        |   did_index
        | )
        | VALUES (
        |   ${row.did},
        |   ${row.publicationStatus},
        |   ${row.atalaOperationContent},
        |   ${row.publishOperationId},
        |   ${row.createdAt},
        |   ${row.updatedAt},
        |   ${row.keyMode},
        |   ${row.didIndex}
        | )
        """.stripMargin.update

    val operationHash = state.createOperation.toAtalaOperationHash
    val hdKeyValues = (now: Instant) =>
      hdKey.toList.map { case (key, path) => (did, key, path.keyUsage, path.keyIndex, now, operationHash) }
    val insertHdKeyIO =
      Update[(PrismDID, String, VerificationRelationship | InternalKeyPurpose, Int, Instant, Array[Byte])](
        "INSERT INTO public.prism_did_hd_key(did, key_id, key_usage, key_index, created_at, operation_hash) VALUES (?, ?, ?, ?, ?, ?)"
      )

    val txnIO = (now: Instant) =>
      for {
        _ <- insertStateIO(DIDStateRow.from(did, state, now)).run
        _ <- insertHdKeyIO.updateMany(hdKeyValues(now))
      } yield ()

    for {
      now <- Clock.instant
      _ <- txnIO(now).transact(xa)
    } yield ()
  }

  override def updateManagedDID(did: PrismDID, patch: ManagedDIDStatePatch): Task[Unit] = {
    val status = PublicationStatusType.from(patch.publicationState)
    val publishedOperationId = patch.publicationState match {
      case PublicationState.Created()                       => None
      case PublicationState.PublicationPending(operationId) => Some(operationId)
      case PublicationState.Published(operationId)          => Some(operationId)
    }
    val cxnIO = (now: Instant) => sql"""
           | UPDATE public.prism_did_wallet_state
           | SET
           |   publication_status = $status,
           |   publish_operation_id = $publishedOperationId,
           |   updated_at = $now
           | WHERE did = $did
           """.stripMargin.update

    for {
      now <- Clock.instant
      _ <- cxnIO(now).run.transact(xa)
    } yield ()
  }

  override def getMaxDIDIndex(): Task[Option[Int]] = {
    val cxnIO =
      sql"""
           | SELECT MAX(did_index)
           | FROM public.prism_did_wallet_state
           | WHERE did_index IS NOT NULL
           """.stripMargin
        .query[Option[Int]]
        .option

    cxnIO.transact(xa).map(_.flatten)
  }

  override def getHdKeyCounter(did: PrismDID): Task[Option[HdKeyIndexCounter]] = {
    val status: ScheduledDIDOperationStatus = ScheduledDIDOperationStatus.Confirmed
    val cxnIO =
      sql"""
           | SELECT
           |   hd.key_usage AS key_usage,
           |   MAX(hd.key_index) AS key_index
           | FROM public.prism_did_hd_key hd
           |   LEFT JOIN public.prism_did_wallet_state ws ON hd.did = ws.did
           |   LEFT JOIN public.prism_did_update_lineage ul ON hd.operation_hash = ul.operation_hash
           | WHERE
           |   hd.did = $did
           |   AND (ul.status = $status OR (ul.status IS NULL AND hd.operation_hash = sha256(ws.atala_operation_content)))
           | GROUP BY hd.did, hd.key_usage
           """.stripMargin
        .query[(VerificationRelationship | InternalKeyPurpose, Int)]
        .to[List]

    getManagedDIDState(did)
      .map(_.flatMap(_.didIndex))
      .flatMap {
        case None => ZIO.none
        case Some(didIndex) =>
          for {
            keyUsageIndex <- cxnIO.transact(xa)
            keyUsageIndexMap = keyUsageIndex.map { case (k, v) => k -> (v + 1) }.toMap
          } yield Some(
            HdKeyIndexCounter(
              didIndex,
              VerificationRelationshipCounter(
                authentication = keyUsageIndexMap.getOrElse(VerificationRelationship.Authentication, 0),
                assertionMethod = keyUsageIndexMap.getOrElse(VerificationRelationship.AssertionMethod, 0),
                keyAgreement = keyUsageIndexMap.getOrElse(VerificationRelationship.KeyAgreement, 0),
                capabilityInvocation = keyUsageIndexMap.getOrElse(VerificationRelationship.CapabilityInvocation, 0),
                capabilityDelegation = keyUsageIndexMap.getOrElse(VerificationRelationship.CapabilityDelegation, 0),
              ),
              InternalKeyCounter(
                master = keyUsageIndexMap.getOrElse(InternalKeyPurpose.Master, 0),
                revocation = keyUsageIndexMap.getOrElse(InternalKeyPurpose.Revocation, 0),
              )
            )
          )
      }
  }

  override def getHdKeyPath(did: PrismDID, keyId: String): Task[Option[ManagedDIDHdKeyPath]] = {
    val status: ScheduledDIDOperationStatus = ScheduledDIDOperationStatus.Confirmed
    val cxnIO =
      sql"""
           | SELECT
           |   ws.did_index,
           |   hd.key_usage,
           |   hd.key_index
           | FROM public.prism_did_hd_key hd
           |   LEFT JOIN public.prism_did_wallet_state ws ON hd.did = ws.did
           |   LEFT JOIN public.prism_did_update_lineage ul ON hd.operation_hash = ul.operation_hash
           | WHERE
           |   hd.did = $did
           |   AND hd.key_id = $keyId
           |   AND (ul.status = $status OR (ul.status IS NULL AND hd.operation_hash = sha256(ws.atala_operation_content)))
           """.stripMargin
        .query[ManagedDIDHdKeyPath]
        .option

    cxnIO.transact(xa)
  }

  override def listManagedDID(
      offset: Option[Int],
      limit: Option[Int]
  ): Task[(Seq[(PrismDID, ManagedDIDState)], Int)] = {
    val countCxnIO =
      sql"""
        | SELECT COUNT(*)
        | FROM public.prism_did_wallet_state
        """.stripMargin
        .query[Int]
        .unique

    val baseFr =
      sql"""
           | SELECT
           |   did,
           |   publication_status,
           |   atala_operation_content,
           |   publish_operation_id,
           |   created_at,
           |   updated_at,
           |   key_mode,
           |   did_index
           | FROM public.prism_did_wallet_state
           | ORDER BY created_at
           """.stripMargin
    val withOffsetFr = offset.fold(baseFr)(offsetValue => baseFr ++ fr"OFFSET $offsetValue")
    val withOffsetAndLimitFr = limit.fold(withOffsetFr)(limitValue => withOffsetFr ++ fr"LIMIT $limitValue")
    val didsCxnIO =
      withOffsetAndLimitFr
        .query[DIDStateRow]
        .to[List]

    for {
      totalCount <- countCxnIO.transact(xa)
      dids <- didsCxnIO
        .transact(xa)
        .map(_.map(row => row.toDomain.map(row.did -> _)))
        .flatMap(ls => ZIO.foreach(ls)(ZIO.fromTry[(PrismDID, ManagedDIDState)](_)))
    } yield (dids, totalCount)
  }

  override def insertDIDUpdateLineage(did: PrismDID, updateLineage: DIDUpdateLineage): Task[Unit] = {
    val cxnIO =
      sql"""
           | INSERT INTO public.prism_did_update_lineage(
           |   did,
           |   operation_hash,
           |   previous_operation_hash,
           |   status,
           |   operation_id,
           |   created_at,
           |   updated_at
           | )
           | VALUES (
           |   $did,
           |   ${updateLineage.operationHash},
           |   ${updateLineage.previousOperationHash},
           |   ${updateLineage.status},
           |   ${updateLineage.operationId},
           |   ${updateLineage.createdAt},
           |   ${updateLineage.updatedAt}
           | )
           """.stripMargin.update

    cxnIO.run.transact(xa).unit
  }

  override def listUpdateLineage(
      did: Option[PrismDID],
      status: Option[ScheduledDIDOperationStatus]
  ): Task[Seq[DIDUpdateLineage]] = {
    val didFilter = did.map(d => fr"did = $d")
    val statusFilter = status.map(s => fr"status = $s")
    val whereFr = Fragments.whereAndOpt(didFilter, statusFilter)
    val baseFr =
      sql"""
           | SELECT
           |   operation_id,
           |   operation_hash,
           |   previous_operation_hash,
           |   status,
           |   created_at,
           |   updated_at
           | FROM public.prism_did_update_lineage
           """.stripMargin
    val cxnIO = (baseFr ++ whereFr)
      .query[DIDUpdateLineage]
      .to[List]

    cxnIO.transact(xa)
  }

  override def setDIDUpdateLineageStatus(
      operationId: Array[Byte],
      status: ScheduledDIDOperationStatus
  ): Task[Unit] = {
    val cxnIO = (now: Instant) => sql"""
            | UPDATE public.prism_did_update_lineage
            | SET
            |   status = $status,
            |   updated_at = $now
            | WHERE operation_id = $operationId
            """.stripMargin.update

    Clock.instant.flatMap(now => cxnIO(now).run.transact(xa)).unit
  }

}

object JdbcDIDNonSecretStorage {
  val layer: URLayer[Transactor[Task], DIDNonSecretStorage] = ZLayer.fromFunction(new JdbcDIDNonSecretStorage(_))
}
