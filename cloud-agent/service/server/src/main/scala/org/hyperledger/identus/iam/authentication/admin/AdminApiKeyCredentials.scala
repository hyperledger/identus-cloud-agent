package org.hyperledger.identus.iam.authentication.admin

import org.hyperledger.identus.iam.authentication.{AuthenticationError, Credentials}

case class AdminApiKeyAuthenticationError(message: String) extends AuthenticationError

object AdminApiKeyAuthenticationError {
  val invalidAdminApiKey = AdminApiKeyAuthenticationError("Invalid Admin API key in header `x-admin-api-key`")
  val emptyAdminApiKey = AdminApiKeyAuthenticationError("Empty `x-admin-api-key` header provided")
}

case class AdminApiKeyCredentials(apiKey: Option[String]) extends Credentials
