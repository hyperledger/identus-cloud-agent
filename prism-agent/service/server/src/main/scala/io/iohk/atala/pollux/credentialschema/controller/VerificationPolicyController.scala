package io.iohk.atala.pollux.credentialschema.controller

import io.iohk.atala.api.http.model.{Order, Pagination}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.pollux.credentialschema.http.{VerificationPolicy, VerificationPolicyInput, VerificationPolicyPage}
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

trait VerificationPolicyController {
  def createVerificationPolicy(
      ctx: RequestContext,
      in: VerificationPolicyInput
  ): ZIO[WalletAccessContext, ErrorResponse, VerificationPolicy]

  def getVerificationPolicyById(
      ctx: RequestContext,
      id: UUID
  ): ZIO[WalletAccessContext, ErrorResponse, VerificationPolicy]

  def updateVerificationPolicyById(
      ctx: RequestContext,
      id: UUID,
      nonce: Int,
      update: VerificationPolicyInput
  ): ZIO[WalletAccessContext, ErrorResponse, VerificationPolicy]

  def deleteVerificationPolicyById(
      ctx: RequestContext,
      id: UUID
  ): ZIO[WalletAccessContext, ErrorResponse, Unit]

  def lookupVerificationPolicies(
      ctx: RequestContext,
      filter: VerificationPolicy.Filter,
      pagination: Pagination,
      order: Option[Order]
  ): ZIO[WalletAccessContext, ErrorResponse, VerificationPolicyPage]
}
