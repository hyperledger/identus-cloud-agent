package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.{VerificationPolicy, VerificationPolicyConstraint}
import io.iohk.atala.pollux.core.model.error.VerificationPolicyError
import io.iohk.atala.pollux.core.repository.VerificationPolicyRepository
import zio.{Clock, IO, Random, Task, URLayer, ZLayer}

import java.util.UUID

object VerificationPolicyServiceImpl {
  val layer: URLayer[VerificationPolicyRepository[Task], VerificationPolicyService] =
    ZLayer.fromFunction(VerificationPolicyServiceImpl(_))
}

class VerificationPolicyServiceImpl(
    repository: VerificationPolicyRepository[Task]
) extends VerificationPolicyService {

  private val throwableToVerificationPolicyError: Throwable => VerificationPolicyError =
    VerificationPolicyError.RepositoryError.apply

  override def create(
      name: String,
      description: String,
      constraints: Seq[VerificationPolicyConstraint]
  ): IO[VerificationPolicyError, VerificationPolicy] = {
    for {
      verificationPolicy <- VerificationPolicy.make(name, description, constraints)
      createdVerificationPolicy <- repository.create(verificationPolicy)
    } yield createdVerificationPolicy
  }.mapError(throwableToVerificationPolicyError)

  override def get(id: UUID): IO[VerificationPolicyError, Option[VerificationPolicy]] =
    repository
      .get(id)
      .mapError(throwableToVerificationPolicyError)

  override def update(
      id: UUID,
      nonce: Int,
      verificationPolicy: VerificationPolicy
  ): IO[VerificationPolicyError, Option[VerificationPolicy]] =
    repository
      .update(id, nonce, verificationPolicy)
      .mapError(throwableToVerificationPolicyError)

  override def delete(id: UUID): IO[VerificationPolicyError, Option[VerificationPolicy]] =
    repository
      .delete(id)
      .mapError(throwableToVerificationPolicyError)

  override def totalCount(): IO[VerificationPolicyError, Long] =
    repository.totalCount().mapError(throwableToVerificationPolicyError)

  override def filteredCount(
      name: Option[String]
  ): IO[VerificationPolicyError, Long] =
    repository.filteredCount(name).mapError(throwableToVerificationPolicyError)

  override def lookup(
      name: Option[String],
      offset: Option[Int],
      limit: Option[Int]
  ): IO[VerificationPolicyError, List[VerificationPolicy]] =
    repository
      .lookup(name, offset, limit)
      .mapError(throwableToVerificationPolicyError)
}
