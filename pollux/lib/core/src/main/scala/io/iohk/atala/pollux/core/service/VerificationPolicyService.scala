package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.error.VerificationPolicyError
import io.iohk.atala.pollux.core.model.error.VerificationPolicyError.*
import io.iohk.atala.pollux.core.model.{VerificationPolicy, VerificationPolicyConstraint}
import zio.IO

import java.util.UUID

trait VerificationPolicyService {

  def create(
      name: String,
      description: String,
      constraints: Seq[VerificationPolicyConstraint] = Seq.empty
  ): IO[VerificationPolicyError, VerificationPolicy]

  def get(id: UUID): IO[VerificationPolicyError, Option[VerificationPolicy]]

  def getHash(id: UUID): IO[VerificationPolicyError, Option[Int]]

  def update(
      id: UUID,
      hash: Int,
      verificationPolicy: VerificationPolicy
  ): IO[VerificationPolicyError, Option[VerificationPolicy]]

  def delete(id: UUID, hash: Int): IO[VerificationPolicyError, Option[VerificationPolicy]]

  def totalCount(): IO[VerificationPolicyError, Long]

  def filteredCount(name: Option[String]): IO[VerificationPolicyError, Long]

  def lookup(
      name: Option[String],
      offset: Option[Int],
      limit: Option[Int]
  ): IO[VerificationPolicyError, List[VerificationPolicy]]
}
