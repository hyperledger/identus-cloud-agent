package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.error.VerificationPolicyError
import io.iohk.atala.pollux.core.model.{VerificationPolicy, VerificationPolicyConstraint}
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

trait VerificationPolicyService {

  def create(
      name: String,
      description: String,
      constraints: Seq[VerificationPolicyConstraint] = Seq.empty
  ): ZIO[WalletAccessContext, VerificationPolicyError, VerificationPolicy]

  def get(id: UUID): ZIO[WalletAccessContext, VerificationPolicyError, Option[VerificationPolicy]]

  def update(
      id: UUID,
      nonce: Int,
      verificationPolicy: VerificationPolicy
  ): ZIO[WalletAccessContext, VerificationPolicyError, Option[VerificationPolicy]]

  def delete(id: UUID): ZIO[WalletAccessContext, VerificationPolicyError, Option[VerificationPolicy]]

  def totalCount(): ZIO[WalletAccessContext, VerificationPolicyError, Long]

  def filteredCount(name: Option[String]): ZIO[WalletAccessContext, VerificationPolicyError, Long]

  def lookup(
      name: Option[String],
      offset: Option[Int],
      limit: Option[Int]
  ): ZIO[WalletAccessContext, VerificationPolicyError, List[VerificationPolicy]]
}
