package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.{VerificationPolicy, VerificationPolicyConstraint}

import java.util.UUID

trait VerificationPolicyRepository[F[_]] {

  def create(verificationPolicy: VerificationPolicy): F[VerificationPolicy]

  def get(id: UUID): F[Option[VerificationPolicy]]

  def exists(id: UUID): F[Boolean]

  def getHash(id: UUID): F[Option[Int]]

  def update(
      id: UUID,
      nonce: Int,
      verificationPolicy: VerificationPolicy
  ): F[Option[VerificationPolicy]]

  def delete(id: UUID, hash: Int): F[Option[VerificationPolicy]]

  def totalCount(): F[Long]

  def filteredCount(nameOpt: Option[String]): F[Long]

  def lookup(
      nameOpt: Option[String],
      offsetOpt: Option[Int],
      limitOpt: Option[Int]
  ): F[List[VerificationPolicy]]
}
