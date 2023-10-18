package io.iohk.atala.iam.authentication.admin

import io.iohk.atala.iam.authentication.{AuthenticationError, Credentials}

case class AdminApiKeyAuthenticationError(message: String) extends AuthenticationError

object AdminApiKeyAuthenticationError {
  val invalidAdminApiKey = AdminApiKeyAuthenticationError("Invalid Admin API key in header `x-admin-api-key`")
}

case class AdminApiKeyCredentials(apiKey: String) extends Credentials
