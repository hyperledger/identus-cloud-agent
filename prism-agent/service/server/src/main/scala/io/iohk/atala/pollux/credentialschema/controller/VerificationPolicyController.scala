package io.iohk.atala.pollux.credentialschema.controller

import io.iohk.atala.api.http.model.{CollectionStats, Order, Pagination, PaginationInput}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.pollux.credentialschema.http.{VerificationPolicy, VerificationPolicyInput, VerificationPolicyPage}
import zio.{IO, Task, ZIO, ZLayer}

import java.util.UUID

trait VerificationPolicyController {
  def createVerificationPolicy(
      ctx: RequestContext,
      in: VerificationPolicyInput
  ): IO[ErrorResponse, VerificationPolicy]

  def getVerificationPolicyById(
      ctx: RequestContext,
      id: UUID
  ): IO[ErrorResponse, VerificationPolicy]

  def updateVerificationPolicyById(
      ctx: RequestContext,
      id: UUID,
      nonce: Int,
      update: VerificationPolicyInput
  ): IO[ErrorResponse, VerificationPolicy]

  def deleteVerificationPolicyById(
      ctx: RequestContext,
      id: UUID
  ): IO[ErrorResponse, Unit]

  def lookupVerificationPolicies(
      ctx: RequestContext,
      filter: VerificationPolicy.Filter,
      pagination: Pagination,
      order: Option[Order]
  ): IO[ErrorResponse, VerificationPolicyPage]
}
