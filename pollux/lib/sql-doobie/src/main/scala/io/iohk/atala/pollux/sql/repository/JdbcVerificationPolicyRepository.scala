package io.iohk.atala.pollux.sql.repository

import cats.syntax.functor.*
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import io.iohk.atala.pollux.core.model
import io.iohk.atala.pollux.core.model.{CredentialSchemaAndTrustedIssuersConstraint, VerificationPolicy}
import io.iohk.atala.pollux.core.repository.VerificationPolicyRepository
import io.iohk.atala.pollux.sql.model.db
import zio.interop.catz.*
import zio.interop.catz.implicits.*
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
  val layer: URLayer[Transactor[Task], VerificationPolicyRepository[Task]] =
    ZLayer.fromFunction(JdbcVerificationPolicyRepository(_))
}

class JdbcVerificationPolicyRepository(xa: Transactor[Task]) extends VerificationPolicyRepository[Task] {
  import VerificationPolicyExtensions.*
  import io.iohk.atala.pollux.sql.model.db.VerificationPolicySql
  override def create(verificationPolicy: model.VerificationPolicy): Task[model.VerificationPolicy] = {
    val program = for {
      vp <- VerificationPolicySql.insert(verificationPolicy.toDto)
      vpc <- VerificationPolicySql.insertConstraints(verificationPolicy.toDtoConstraints)
    } yield vp.toDomain(vpc)

    for {
      vp: model.VerificationPolicy <- program.transact(xa)
    } yield vp
  }

  override def get(id: UUID): Task[Option[model.VerificationPolicy]] = {
    val program = for {
      vp <- VerificationPolicySql.getById(id)
      vpc <- VerificationPolicySql.getVerificationPolicyConstrains(Seq(id))
    } yield vp.map(_.toDomain(vpc))

    for {
      vp: Option[model.VerificationPolicy] <- program.transact(xa)
    } yield vp
  }

  override def exists(id: UUID): Task[Boolean] =
    VerificationPolicySql.exists(id).transact(xa)

  override def getHash(id: UUID): Task[Option[Int]] =
    VerificationPolicySql.getHashById(id).transact(xa)

  override def update(
      id: UUID,
      nonce: Int,
      verificationPolicy: model.VerificationPolicy
  ): Task[Option[model.VerificationPolicy]] = {
    val preparedVP = verificationPolicy.copy(id = id, updatedAt = OffsetDateTime.now(ZoneOffset.UTC))
    val program = for {
      _ <- VerificationPolicySql.update(preparedVP.toDto, nonce)
      _ <- VerificationPolicySql.dropConstraintsByVerificationPolicyId(id)
      vp: Option[db.VerificationPolicy] <- VerificationPolicySql.getById(id)
      vpc: Seq[db.VerificationPolicyConstraint] <- VerificationPolicySql.insertConstraints(
        preparedVP.toDtoConstraints
      )
    } yield vp.map(_.toDomain(vpc))

    program.transact(xa)
  }

  override def delete(id: UUID): Task[Option[model.VerificationPolicy]] = {
    val program = for {
      vp <- VerificationPolicySql.getById(id)
      vpc <- VerificationPolicySql.getVerificationPolicyConstrains(Seq(id))
      _ <- VerificationPolicySql.delete(id)
    } yield vp.map(_.toDomain(vpc))

    program.transact(xa)
  }

  override def totalCount(): Task[Long] = {
    VerificationPolicySql.count().transact(xa)
  }

  override def filteredCount(nameOpt: Option[String]): Task[Long] =
    VerificationPolicySql.countFiltered(nameOpt).transact(xa)

  override def lookup(
      nameOpt: Option[String],
      offsetOpt: Option[Int],
      limitOpt: Option[Int]
  ): Task[List[model.VerificationPolicy]] = {
    for {
      policies: List[db.VerificationPolicy] <- VerificationPolicySql
        .filteredVerificationPolicies(nameOpt, offsetOpt, limitOpt)
        .transact(xa)
      ids = policies.map(_.id)

      constrains <- VerificationPolicySql
        .getVerificationPolicyConstrains(ids)
        .transact(xa)

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
