package io.iohk.atala.multitenancy.sql.repository

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.multitenancy.core.model.*
import io.iohk.atala.multitenancy.core.model.error.DidWalletMappingRepositoryError.*
import io.iohk.atala.multitenancy.core.repository.DidWalletMappingRepository
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.db.Implicits.{*, given}
import io.iohk.atala.shared.models.WalletId
import org.postgresql.util.PSQLException
import zio.*

import java.time.Instant
import java.util.UUID

class JdbcDidWalletMappingRepository(xa: Transactor[ContextAwareTask]) extends DidWalletMappingRepository[Task] {

  given uuidGet: Get[UUID] = Get[String].map(UUID.fromString)
  given uuidPut: Put[UUID] = Put[String].contramap(_.toString())

  override def createDidWalletMappingRecord(record: DidWalletMappingRecord): Task[Int] = {
    val cxnIO = sql"""
        | INSERT INTO public.did_wallet_id_mapping(
        |   did,
        |   wallet_id,
        |   created_at,
        |   updated_at
        | ) values (
        |   ${record.did},
        |   ${record.walletId},
        |   ${record.createdAt},
        |   ${record.updatedAt}
        | )
        """.stripMargin.update

    cxnIO.run
      .transact(xa)
      .mapError {
        case e: PSQLException => {
          UniqueConstraintViolation(e.getMessage)
        }
        case e => e
      }
  }

  override def getDidWalletMappingRecords: Task[Seq[DidWalletMappingRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   did,
        |   wallet_id,
        |   created_at,
        |   updated_at
        | FROM public.did_wallet_id_mapping
        """.stripMargin
      .query[DidWalletMappingRecord]
      .to[Seq]

    cxnIO
      .transact(xa)
  }

  override def deleteDidWalletMappingByDid(did: DidId): Task[Int] = {
    val cxnIO = sql"""
      | DELETE
      | FROM public.did_wallet_id_mapping
      | WHERE did = $did
      """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def deleteDidWalletMappingByWalletId(walletId: WalletId): Task[Int] = {
    val cxnIO =
      sql"""
           | DELETE
           | FROM public.did_wallet_id_mapping
           | WHERE wallet_id = $walletId
      """.stripMargin.update

    cxnIO.run
      .transact(xa)
  }

  override def getDidWalletMappingByWalletId(walletId: WalletId): Task[Seq[DidWalletMappingRecord]] = {
    val cxnIO = sql"""
        | SELECT
        |   did,
        |   wallet_id,
        |   created_at,
        |   updated_at
        | FROM public.did_wallet_id_mapping
        | WHERE wallet_id = $walletId
        """.stripMargin
      .query[DidWalletMappingRecord]
      .to[Seq]

    cxnIO
      .transact(xa)
  }

  override def getDidWalletMappingByDid(did: DidId): Task[Option[DidWalletMappingRecord]] = {
    val cxnIO = sql"""
                     | SELECT
                     |   did,
                     |   wallet_id,
                     |   created_at,
                     |   updated_at
                     | FROM public.did_wallet_id_mapping
                     | WHERE did = $did
        """.stripMargin
      .query[DidWalletMappingRecord]
      .option

    cxnIO
      .transact(xa)
  }

}

object JdbcDidWalletMappingRepository {
  val layer: URLayer[Transactor[ContextAwareTask], DidWalletMappingRepository[Task]] =
    ZLayer.fromFunction(new JdbcDidWalletMappingRepository(_))
}
