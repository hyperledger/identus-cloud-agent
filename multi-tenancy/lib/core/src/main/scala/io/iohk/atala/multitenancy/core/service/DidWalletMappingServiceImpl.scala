package io.iohk.atala.multitenancy.core.service

import io.iohk.atala.mercury.*
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.multitenancy.core.model.DidWalletMappingRecord
import io.iohk.atala.multitenancy.core.model.error.DidWalletMappingServiceError
import io.iohk.atala.multitenancy.core.model.error.DidWalletMappingServiceError.*
import io.iohk.atala.multitenancy.core.repository.DidWalletMappingRepository
import io.iohk.atala.shared.models.WalletId
import zio.*

import java.rmi.UnexpectedException
import java.time.Instant
import java.util.UUID

private class DidWalletMappingServiceImpl(
    didWalletMappingRepository: DidWalletMappingRepository[Task],
) extends DidWalletMappingService {

  override def createDidWalletMapping(
      did: DidId,
      walletId: WalletId
  ): IO[DidWalletMappingServiceError, DidWalletMappingRecord] =
    for {
      record <- ZIO.succeed(
        DidWalletMappingRecord(
          createdAt = Instant.now,
          updatedAt = None,
          did = did,
          walletId = walletId
        )
      )
      count <- didWalletMappingRepository
        .createDidWalletMappingRecord(record)
        .flatMap {
          case 1 => ZIO.succeed(())
          case n => ZIO.fail(UnexpectedException(s"Invalid row count result: $n"))
        }
        .mapError(RepositoryError.apply)
    } yield record

  override def getDidWalletMappingRecords: IO[DidWalletMappingServiceError, Seq[DidWalletMappingRecord]] = {
    for {
      records <- didWalletMappingRepository.getDidWalletMappingRecords
        .mapError(RepositoryError.apply)
    } yield records
  }

  override def getDidWalletMappingByWalletId(
      walletId: WalletId
  ): IO[DidWalletMappingServiceError, Seq[DidWalletMappingRecord]] =
    for {
      record <- didWalletMappingRepository
        .getDidWalletMappingByWalletId(walletId)
        .mapError(RepositoryError.apply)
    } yield record

  override def getDidWalletMappingByDid(did: DidId): IO[DidWalletMappingServiceError, Option[DidWalletMappingRecord]] =
    for {
      record <- didWalletMappingRepository
        .getDidWalletMappingByDid(did)
        .mapError(RepositoryError.apply)
    } yield record

  override def deleteDidWalletMappingByDid(didId: DidId): IO[DidWalletMappingServiceError, Int] = ???

  override def deleteDidWalletMappingByWalletId(walletId: WalletId): IO[DidWalletMappingServiceError, Int] = ???

}

object DidWalletMappingServiceImpl {
  val layer: URLayer[DidWalletMappingRepository[Task], DidWalletMappingService] =
    ZLayer.fromFunction(DidWalletMappingServiceImpl(_))
}
