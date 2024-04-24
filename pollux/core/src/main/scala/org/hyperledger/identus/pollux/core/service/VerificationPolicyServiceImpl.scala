package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.error.VerificationPolicyError
import org.hyperledger.identus.pollux.core.model.{VerificationPolicy, VerificationPolicyConstraint}
import org.hyperledger.identus.pollux.core.repository.VerificationPolicyRepository
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

object VerificationPolicyServiceImpl {
  val layer: URLayer[VerificationPolicyRepository, VerificationPolicyService] =
    ZLayer.fromFunction(VerificationPolicyServiceImpl(_))
}

class VerificationPolicyServiceImpl(
    repository: VerificationPolicyRepository
) extends VerificationPolicyService {

  private val throwableToVerificationPolicyError: Throwable => VerificationPolicyError =
    VerificationPolicyError.RepositoryError.apply

  override def create(
      name: String,
      description: String,
      constraints: Seq[VerificationPolicyConstraint]
  ): ZIO[WalletAccessContext, VerificationPolicyError, VerificationPolicy] = {
    for {
      verificationPolicy <- VerificationPolicy.make(name, description, constraints)
      createdVerificationPolicy <- repository.create(verificationPolicy)
    } yield createdVerificationPolicy
  }.mapError(throwableToVerificationPolicyError)

  override def get(id: UUID): ZIO[WalletAccessContext, VerificationPolicyError, Option[VerificationPolicy]] =
    repository
      .get(id)
      .mapError(throwableToVerificationPolicyError)

  override def update(
      id: UUID,
      nonce: Int,
      verificationPolicy: VerificationPolicy
  ): ZIO[WalletAccessContext, VerificationPolicyError, Option[VerificationPolicy]] =
    repository
      .update(id, nonce, verificationPolicy)
      .mapError(throwableToVerificationPolicyError)

  override def delete(id: UUID): ZIO[WalletAccessContext, VerificationPolicyError, Option[VerificationPolicy]] =
    repository
      .delete(id)
      .mapError(throwableToVerificationPolicyError)

  override def totalCount(): ZIO[WalletAccessContext, VerificationPolicyError, Long] =
    repository.totalCount().mapError(throwableToVerificationPolicyError)

  override def filteredCount(
      name: Option[String]
  ): ZIO[WalletAccessContext, VerificationPolicyError, Long] =
    repository.filteredCount(name).mapError(throwableToVerificationPolicyError)

  override def lookup(
      name: Option[String],
      offset: Option[Int],
      limit: Option[Int]
  ): ZIO[WalletAccessContext, VerificationPolicyError, List[VerificationPolicy]] =
    repository
      .lookup(name, offset, limit)
      .mapError(throwableToVerificationPolicyError)
}
