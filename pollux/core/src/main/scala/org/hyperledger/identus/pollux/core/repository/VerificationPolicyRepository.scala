package org.hyperledger.identus.pollux.core.repository

import org.hyperledger.identus.pollux.core.model.VerificationPolicy
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

trait VerificationPolicyRepository {

  def create(verificationPolicy: VerificationPolicy): RIO[WalletAccessContext, VerificationPolicy]

  def get(id: UUID): RIO[WalletAccessContext, Option[VerificationPolicy]]

  def exists(id: UUID): RIO[WalletAccessContext, Boolean]

  def getHash(id: UUID): RIO[WalletAccessContext, Option[Int]]

  def update(
      id: UUID,
      nonce: Int,
      verificationPolicy: VerificationPolicy
  ): RIO[WalletAccessContext, Option[VerificationPolicy]]

  def delete(id: UUID): RIO[WalletAccessContext, Option[VerificationPolicy]]

  def totalCount(): RIO[WalletAccessContext, Long]

  def filteredCount(nameOpt: Option[String]): RIO[WalletAccessContext, Long]

  def lookup(
      nameOpt: Option[String],
      offsetOpt: Option[Int],
      limitOpt: Option[Int]
  ): RIO[WalletAccessContext, List[VerificationPolicy]]
}
