package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.iohk.atala.agent.walletapi.model.{DIDUpdateLineage, ManagedDIDState}
import io.iohk.atala.agent.walletapi.storage.DIDNonSecretStorage
import io.iohk.atala.castor.core.model.did.{PrismDID, ScheduledDIDOperationStatus}
import zio.*
import zio.interop.catz.*

import java.time.Instant

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
        |   key_mode
        | FROM public.prism_did_wallet_state
        | WHERE did = $did
        """.stripMargin
        .query[DIDStateRow]
        .option

    cxnIO
      .transact(xa)
      .flatMap(_.map(_.toDomain).fold(ZIO.none)(t => ZIO.fromTry(t).asSome))
  }

  override def setManagedDIDState(did: PrismDID, state: ManagedDIDState): Task[Unit] = {
    val cxnIO = (row: DIDStateRow) => sql"""
        | INSERT INTO public.prism_did_wallet_state(
        |   did,
        |   publication_status,
        |   atala_operation_content,
        |   publish_operation_id,
        |   created_at,
        |   updated_at,
        |   key_mode
        | )
        | VALUES (
        |   ${row.did},
        |   ${row.publicationStatus},
        |   ${row.atalaOperationContent},
        |   ${row.publishOperationId},
        |   ${row.createdAt},
        |   ${row.updatedAt},
        |   ${row.keyMode}
        | )
        | ON CONFLICT (did) DO UPDATE SET
        |   publication_status = EXCLUDED.publication_status,
        |   atala_operation_content = EXCLUDED.atala_operation_content,
        |   publish_operation_id = EXCLUDED.publish_operation_id,
        |   updated_at = EXCLUDED.updated_at,
        |   key_mode = EXCLUDED.key_mode
        """.stripMargin.update

    for {
      now <- Clock.instant
      row = DIDStateRow.from(did, state, now)
      _ <- cxnIO(row).run.transact(xa)
    } yield ()
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
           |   key_mode
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
