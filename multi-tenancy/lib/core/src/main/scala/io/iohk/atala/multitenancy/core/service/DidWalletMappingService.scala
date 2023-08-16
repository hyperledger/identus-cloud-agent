package io.iohk.atala.multitenancy.core.service

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.multitenancy.core.model.DidWalletMappingRecord
import io.iohk.atala.multitenancy.core.model.error.DidWalletMappingServiceError
import io.iohk.atala.shared.models.WalletId
import zio.*

trait DidWalletMappingService {

  def createDidWalletMapping(
      did: DidId,
      walletId: WalletId
  ): IO[DidWalletMappingServiceError, DidWalletMappingRecord]

  def getDidWalletMappingRecords: IO[DidWalletMappingServiceError, Seq[DidWalletMappingRecord]]

  def getDidWalletMappingByWalletId(
      walletId: WalletId
  ): IO[DidWalletMappingServiceError, Seq[DidWalletMappingRecord]]

  def getDidWalletMappingByDid(did: DidId): IO[DidWalletMappingServiceError, Option[DidWalletMappingRecord]]

  def deleteDidWalletMappingByDid(did: DidId): IO[DidWalletMappingServiceError, Int]

  def deleteDidWalletMappingByWalletId(walletId: WalletId): IO[DidWalletMappingServiceError, Int]

}
