package io.iohk.atala.pollux.credentialschema.controller

import io.iohk.atala.api.http.model.{CollectionStats, Order, Pagination, PaginationInput}
import io.iohk.atala.api.http.{FailureResponse, RequestContext}
import io.iohk.atala.pollux.credentialschema.http.{VerificationPolicy, VerificationPolicyInput, VerificationPolicyPage}
import zio.{IO, Task, ZIO, ZLayer}

import java.util.UUID

trait VerificationPolicyController {
  def createVerificationPolicy(
      ctx: RequestContext,
      in: VerificationPolicyInput
  ): IO[FailureResponse, VerificationPolicy]

  def getVerificationPolicyById(
      ctx: RequestContext,
      id: UUID
  ): IO[FailureResponse, VerificationPolicy]

  def updateVerificationPolicyById(
      ctx: RequestContext,
      id: UUID,
      nonce: Int,
      update: VerificationPolicyInput
  ): IO[FailureResponse, VerificationPolicy]

  def deleteVerificationPolicyById(
      ctx: RequestContext,
      id: UUID,
      nonce: Int
  ): IO[FailureResponse, Unit]

  def lookupVerificationPolicies(
      ctx: RequestContext,
      filter: VerificationPolicy.Filter,
      pagination: Pagination,
      order: Option[Order]
  ): IO[FailureResponse, VerificationPolicyPage]
}
