package io.iohk.atala.multitenancy.core.repository

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.multitenancy.core.model.DidWalletMappingRecord
import io.iohk.atala.shared.models.WalletId

trait DidWalletMappingRepository[F[_]] {
  def createDidWalletMappingRecord(record: DidWalletMappingRecord): F[Int]

  def getDidWalletMappingRecords: F[Seq[DidWalletMappingRecord]]

  def deleteDidWalletMappingByDid(did: DidId): F[Int]

  def deleteDidWalletMappingByWalletId(walletId: WalletId): F[Int]

  def getDidWalletMappingByWalletId(walletId: WalletId): F[Seq[DidWalletMappingRecord]]

  def getDidWalletMappingByDid(did: DidId): F[Option[DidWalletMappingRecord]]
}
