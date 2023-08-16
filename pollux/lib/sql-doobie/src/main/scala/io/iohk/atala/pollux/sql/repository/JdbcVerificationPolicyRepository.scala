package io.iohk.atala.pollux.sql.repository

import doobie.*
import doobie.util.transactor.Transactor
import io.iohk.atala.pollux.core.model
import io.iohk.atala.pollux.core.model.{CredentialSchemaAndTrustedIssuersConstraint, VerificationPolicy}
import io.iohk.atala.pollux.core.repository.VerificationPolicyRepository
import io.iohk.atala.pollux.sql.model.db
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.db.Implicits.*
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

object VerificationPolicyExtensions {

  extension (vp: model.VerificationPolicy) {
    def toDto = db.VerificationPolicy(
      id = vp.id,
      name = vp.name,
      nonce = vp.nonce,
      description = vp.description,
      createdAt = vp.createdAt,
      updatedAt = vp.updatedAt
    )

    def toDtoConstraints: Seq[db.VerificationPolicyConstraint] = vp.constrains.zipWithIndex.map((vpc, index) =>
      vpc match {
        case CredentialSchemaAndTrustedIssuersConstraint(schemaId, trustedIssuers) =>
          db.VerificationPolicyConstraint(
            fk_id = vp.id,
            index = index,
            `type` = vpc.getClass.getSimpleName,
            schemaId = schemaId,
            trustedIssuers = trustedIssuers
          )
      }
    )
  }

  extension (vp: db.VerificationPolicy) {
    def toDomain(constraints: Seq[db.VerificationPolicyConstraint]): model.VerificationPolicy =
      model.VerificationPolicy(
        id = vp.id,
        name = vp.name,
        description = vp.description,
        createdAt = vp.createdAt.atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime,
        updatedAt = vp.updatedAt.atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime,
        constrains = constraints.map(_.toDomain),
        nonce = vp.nonce
      )
  }

  extension (vpc: db.VerificationPolicyConstraint) {
    def toDomain: model.VerificationPolicyConstraint = {
      vpc.`type` match
        case "CredentialSchemaAndTrustedIssuersConstraint" =>
          model.CredentialSchemaAndTrustedIssuersConstraint(vpc.schemaId, vpc.trustedIssuers)
    }
  }
}

object JdbcVerificationPolicyRepository {
  val layer: URLayer[Transactor[ContextAwareTask], VerificationPolicyRepository] =
    ZLayer.fromFunction(JdbcVerificationPolicyRepository(_))
}

class JdbcVerificationPolicyRepository(xa: Transactor[ContextAwareTask]) extends VerificationPolicyRepository {
  import VerificationPolicyExtensions.*
  import io.iohk.atala.pollux.sql.model.db.VerificationPolicySql

  override def create(
      verificationPolicy: model.VerificationPolicy
  ): RIO[WalletAccessContext, model.VerificationPolicy] = {
    val program = for {
      vp <- VerificationPolicySql.insert(verificationPolicy.toDto)
      vpc <- VerificationPolicySql.insertConstraints(verificationPolicy.toDtoConstraints)
    } yield vp.toDomain(vpc)

    for {
      vp: model.VerificationPolicy <- program.transactWallet(xa)
    } yield vp
  }

  override def get(id: UUID): RIO[WalletAccessContext, Option[model.VerificationPolicy]] = {
    val program = for {
      vp <- VerificationPolicySql.getById(id)
      vpc <- VerificationPolicySql.getVerificationPolicyConstrains(Seq(id))
    } yield vp.map(_.toDomain(vpc))

    for {
      vp: Option[model.VerificationPolicy] <- program.transactWallet(xa)
    } yield vp
  }

  override def exists(id: UUID): RIO[WalletAccessContext, Boolean] =
    VerificationPolicySql.exists(id).transactWallet(xa)

  override def getHash(id: UUID): RIO[WalletAccessContext, Option[Int]] =
    VerificationPolicySql.getHashById(id).transactWallet(xa)

  override def update(
      id: UUID,
      nonce: Int,
      verificationPolicy: model.VerificationPolicy
  ): RIO[WalletAccessContext, Option[model.VerificationPolicy]] = {
    val preparedVP = verificationPolicy.copy(id = id, updatedAt = OffsetDateTime.now(ZoneOffset.UTC))
    val program = for {
      _ <- VerificationPolicySql.update(preparedVP.toDto, nonce)
      _ <- VerificationPolicySql.dropConstraintsByVerificationPolicyId(id)
      vp: Option[db.VerificationPolicy] <- VerificationPolicySql.getById(id)
      vpc: Seq[db.VerificationPolicyConstraint] <- VerificationPolicySql.insertConstraints(
        preparedVP.toDtoConstraints
      )
    } yield vp.map(_.toDomain(vpc))

    program.transactWallet(xa)
  }

  override def delete(id: UUID): RIO[WalletAccessContext, Option[model.VerificationPolicy]] = {
    val program = for {
      vp <- VerificationPolicySql.getById(id)
      vpc <- VerificationPolicySql.getVerificationPolicyConstrains(Seq(id))
      _ <- VerificationPolicySql.delete(id)
    } yield vp.map(_.toDomain(vpc))

    program.transactWallet(xa)
  }

  override def totalCount(): RIO[WalletAccessContext, Long] = {
    VerificationPolicySql.count().transactWallet(xa)
  }

  override def filteredCount(nameOpt: Option[String]): RIO[WalletAccessContext, Long] =
    VerificationPolicySql.countFiltered(nameOpt).transactWallet(xa)

  override def lookup(
      nameOpt: Option[String],
      offsetOpt: Option[Int],
      limitOpt: Option[Int]
  ): RIO[WalletAccessContext, List[model.VerificationPolicy]] = {
    for {
      policies: List[db.VerificationPolicy] <- VerificationPolicySql
        .filteredVerificationPolicies(nameOpt, offsetOpt, limitOpt)
        .transactWallet(xa)
      ids = policies.map(_.id)

      constrains <- VerificationPolicySql
        .getVerificationPolicyConstrains(ids)
        .transactWallet(xa)

      constraintsById = constrains.groupBy(_.fk_id)

      domainPolicies = policies.map(policy =>
        policy.toDomain(
          constraintsById
            .getOrElse(policy.id, List.empty)
            .sortBy(_.index)
            .toVector
        )
      )
    } yield domainPolicies
  }
}
