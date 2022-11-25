package io.iohk.atala.pollux.service

import io.iohk.atala.api.http.model.{Order, Pagination}
import io.iohk.atala.pollux.schema.model.{VerificationPolicy, VerificationPolicyInput, VerificationPolicyPage}
import zio.{Ref, Task, UIO, ZIO, ZLayer}

import java.time.ZonedDateTime
import scala.collection.mutable

class VerificationPolicyServiceInMemory(
    ref: Ref[Map[String, VerificationPolicy]]
) extends VerificationPolicyService {

  // TODO: Figure out what is the logic for trying to overwrite the schema with the same id (409 Conflict)
  // TODO: Other validations (same [schema_name, version], list of the attributes is not empty, etc)
  override def createVerificationPolicy(
      in: VerificationPolicyInput
  ): Task[VerificationPolicy] = {
    val vp = VerificationPolicy(in)
    for {
      _ <- ref.update(s => s + (vp.id -> vp))
    } yield vp
  }

  override def getVerificationPolicyById(
      id: String
  ): Task[Option[VerificationPolicy]] = {
    for {
      storage <- ref.get
      vp = storage.get(id)
    } yield vp
  }

  override def updateVerificationPolicyById(
      id: String,
      in: VerificationPolicyInput
  ): Task[Option[VerificationPolicy]] = {
    for {
      storage: Map[String, VerificationPolicy] <- ref.updateAndGet(kv =>
        kv.get(id)
          .fold(kv)(oldVp => kv + (id -> oldVp.update(in)))
      )
      vp = storage.get(id)
    } yield vp
  }

  override def deleteVerificationPolicyById(
      id: String
  ): Task[Option[VerificationPolicy]] = {
    for {
      storage: Map[String, VerificationPolicy] <- ref.getAndUpdate(kv =>
        kv.get(id)
          .fold(kv)(_ => kv - id)
      )
      vp = storage.get(id)
    } yield vp
  }

  // TODO: this is naive implementation for demo purposes, sorting doesn't work
  override def lookupVerificationPolicies(
      filter: VerificationPolicy.Filter,
      pagination: Pagination,
      order: Option[Order]
  ): Task[VerificationPolicyPage] = {
    for {
      storage: Map[String, VerificationPolicy] <- ref.get
      filtered = storage.values.filter(filter.predicate)
      paginated = filtered.toList
        .slice(
          pagination.offset.getOrElse(0),
          pagination.offset.getOrElse(0) + pagination.limit.getOrElse(10)
        )
    } yield VerificationPolicyPage(
      self = "to be defined",
      kind = "VerifiableCredentialSchema",
      pageOf = "to be defined",
      next = None,
      previous = None,
      contents = paginated
    )
  }
}

object VerificationPolicyServiceInMemory {
  val layer = ZLayer.fromZIO(
    Ref
      .make(Map.empty[String, VerificationPolicy])
      .map(VerificationPolicyServiceInMemory(_))
  )
}
