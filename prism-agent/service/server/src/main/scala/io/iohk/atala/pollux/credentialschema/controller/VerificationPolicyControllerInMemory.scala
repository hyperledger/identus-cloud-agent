package io.iohk.atala.pollux.credentialschema.controller

import io.iohk.atala.api.http.model.{CollectionStats, Order, Pagination, PaginationInput}
import io.iohk.atala.api.http.{FailureResponse, RequestContext}
import io.iohk.atala.pollux.credentialschema.controller.{
  VerificationPolicyController,
  VerificationPolicyPageRequestLogic
}
import io.iohk.atala.pollux.credentialschema.http.{VerificationPolicy, VerificationPolicyInput, VerificationPolicyPage}
import zio.{IO, Ref, Task, UIO, ULayer, ZIO, ZLayer}

import java.time.ZonedDateTime
import java.util.UUID
import scala.collection.mutable

class VerificationPolicyControllerInMemory(
    ref: Ref[Map[UUID, VerificationPolicy]]
) extends VerificationPolicyController {

  // TODO: Figure out what is the logic for trying to overwrite the schema with the same id (409 Conflict)
  // TODO: Other validations (same [schema_name, version], list of the attributes is not empty, etc)
  override def createVerificationPolicy(
      ctx: RequestContext,
      in: VerificationPolicyInput
  ): IO[FailureResponse, VerificationPolicy] = {
    val vp = VerificationPolicy(in)
    for {
      _ <- ref.update(s => s + (vp.id -> vp))
    } yield vp
  }

  override def getVerificationPolicyById(
      ctx: RequestContext,
      id: UUID
  ): IO[FailureResponse, VerificationPolicy] = {
    for {
      storage <- ref.get
      vp = storage.get(id)
    } yield vp.get
  }

  override def updateVerificationPolicyById(
      ctx: RequestContext,
      id: UUID,
      nonce: Int,
      in: VerificationPolicyInput
  ): IO[FailureResponse, VerificationPolicy] = {
    for {
      storage: Map[UUID, VerificationPolicy] <- ref.updateAndGet(kv =>
        kv.get(id)
          .fold(kv)(oldVp => kv + (id -> oldVp.update(in)))
      )
      vp = storage.get(id)
    } yield vp.get
  }

  override def deleteVerificationPolicyById(
      ctx: RequestContext,
      id: UUID,
      nonce: Int
  ): IO[FailureResponse, Unit] = {
    for {
      storage: Map[UUID, VerificationPolicy] <- ref.getAndUpdate(kv =>
        kv.get(id)
          .fold(kv)(_ => kv - id)
      )
      vp = storage.get(id)
    } yield ()
  }

  // TODO: this is naive implementation for demo purposes, sorting doesn't work
  override def lookupVerificationPolicies(
      ctx: RequestContext,
      filter: VerificationPolicy.Filter,
      pagination: Pagination,
      order: Option[Order]
  ): IO[FailureResponse, VerificationPolicyPage] = {
    for {
      storage: Map[UUID, VerificationPolicy] <- ref.get
      totalCount = storage.count(_ => true)
      filtered = storage.values.filter(filter.predicate).toList
      filteredCount = filtered.length
      paginated = filtered
        .slice(
          pagination.offset,
          pagination.offset + pagination.limit
        )
    } yield VerificationPolicyPageRequestLogic(
      ctx,
      pagination,
      paginated,
      CollectionStats(totalCount, filteredCount)
    ).result
  }
}

object VerificationPolicyControllerInMemory {
  val layer: ULayer[VerificationPolicyController] = ZLayer.fromZIO(
    Ref
      .make(Map.empty[UUID, VerificationPolicy])
      .map(VerificationPolicyControllerInMemory(_))
  )
}
