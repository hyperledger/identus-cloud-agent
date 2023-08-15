package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.VerificationPolicy
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

trait VerificationPolicyRepository {

  def create(verificationPolicy: VerificationPolicy): RIO[WalletAccessContext, VerificationPolicy]

  def get(id: UUID): Task[Option[VerificationPolicy]]

  def exists(id: UUID): Task[Boolean]

  def getHash(id: UUID): Task[Option[Int]]

  def update(
      id: UUID,
      nonce: Int,
      verificationPolicy: VerificationPolicy
  ): Task[Option[VerificationPolicy]]

  def delete(id: UUID): Task[Option[VerificationPolicy]]

  def totalCount(): Task[Long]

  def filteredCount(nameOpt: Option[String]): Task[Long]

  def lookup(
      nameOpt: Option[String],
      offsetOpt: Option[Int],
      limitOpt: Option[Int]
  ): Task[List[VerificationPolicy]]
}
