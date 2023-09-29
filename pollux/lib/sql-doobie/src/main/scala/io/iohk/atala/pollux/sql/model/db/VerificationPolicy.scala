package io.iohk.atala.pollux.sql.model.db

import io.getquill.*
import io.getquill.doobie.DoobieContext
import io.iohk.atala.shared.models.WalletId
import java.util.UUID

import java.time.OffsetDateTime

case class VerificationPolicy(
    id: UUID,
    nonce: Int,
    name: String,
    description: String,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    walletId: WalletId
)

case class VerificationPolicyConstraint(
    fk_id: UUID,
    index: Int,
    `type`: String,
    schemaId: String,
    trustedIssuers: Seq[String]
)

object VerificationPolicySql extends DoobieContext.Postgres(SnakeCase) {
  import io.iohk.atala.pollux.sql.repository.VerificationPolicyExtensions._

  def insert(verificationPolicy: VerificationPolicy) = {
    run(quote(query[VerificationPolicy].insertValue(lift(verificationPolicy)).returning(vp => vp)))
  }

  def insertConstraints(constraints: Seq[VerificationPolicyConstraint]) = {
    run(
      quote(liftQuery(constraints).foreach(c => query[VerificationPolicyConstraint].insertValue(c).returning(c => c)))
    )
  }

  def dropConstraintsByVerificationPolicyId(fk_id: UUID) = {
    run(quote(query[VerificationPolicyConstraint].filter(_.fk_id == lift(fk_id)).delete))
  }

  def getById(id: UUID) =
    run(
      quote(
        query[VerificationPolicy]
          .filter(_.id == lift(id))
      )
    ).map(_.headOption)

  def getHashById(id: UUID) =
    run(
      quote(
        query[VerificationPolicy]
          .filter(_.id == lift(id))
          .map(_.nonce)
      )
    ).map(_.headOption)

  def exists(id: UUID) =
    run(quote(query[VerificationPolicy].filter(_.id == lift(id)).isEmpty))

  def exists(id: UUID, hash: Int) =
    run(
      quote(
        query[VerificationPolicy]
          .filter(_.id == lift(id))
          .filter(_.nonce == lift(hash))
          .isEmpty
      )
    )

  def getVerificationPolicyConstrains(fk_ids: Seq[UUID]) =
    run(
      quote(
        query[VerificationPolicyConstraint].filter(vpc => liftQuery(fk_ids).contains(vpc.fk_id))
      )
    )

  def update(verificationPolicy: VerificationPolicy, nonce: Int) =
    run(
      quote(
        query[VerificationPolicy]
          .filter(_.id == lift(verificationPolicy.id))
          .filter(_.nonce == lift(nonce))
          .update(
            _.nonce -> lift(verificationPolicy.nonce),
            _.description -> lift(verificationPolicy.description),
            _.name -> lift(verificationPolicy.name),
            _.updatedAt -> lift(verificationPolicy.updatedAt)
          )
          .returning(vp => vp)
      )
    )

  def delete(id: UUID) =
    run(
      quote(query[VerificationPolicy])
        .filter(_.id == lift(id))
        .delete
        .returning(vp => vp)
    )

  def delete(id: UUID, hash: Int) =
    run(
      quote(query[VerificationPolicy])
        .filter(_.id == lift(id))
        .filter(_.nonce == lift(hash))
        .delete
        .returning(vp => vp)
    )

  def deleteAll() = run(quote(query[VerificationPolicy].delete))

  def deleteConstraints(fk_id: UUID) =
    run(quote(query[VerificationPolicyConstraint]).filter(_.fk_id == lift(fk_id)).delete)

  def count() = run(quote(query[VerificationPolicy].size))

  def countOfConstraints() = run(quote(query[VerificationPolicyConstraint]).size)

  def countFiltered(nameOpt: Option[String]) =
    run(
      quote(query[VerificationPolicy]).dynamic
        .filterOpt(nameOpt)((vp, name) => quote(vp.name.like(name)))
        .size
    )

  def filteredVerificationPolicies(nameOpt: Option[String], offsetOpt: Option[Int], limitOpt: Option[Int]) = run {

    quote(query[VerificationPolicy]).dynamic
      .filterOpt(nameOpt)((vcs, name) => quote(vcs.name.like(name)))
      .sortBy(_.id)
      .dropOpt(offsetOpt)
      .take(limitOpt.getOrElse(1000))
  }
}
