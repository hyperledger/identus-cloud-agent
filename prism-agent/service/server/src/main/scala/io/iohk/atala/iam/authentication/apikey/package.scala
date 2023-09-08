package io.iohk.atala.iam.authentication

package object apikey {
  case class ApiKeyCredentials(apiKey: Option[String]) extends Credentials

  case class ApiKeyAuthenticationError(message: String) extends AuthenticationError

  object ApiKeyAuthenticationError {
    val invalidApiKey = ApiKeyAuthenticationError("Invalid `api-key` header provided")
    val emptyApiKey = ApiKeyAuthenticationError("Empty `api-key` header provided")
  }
}
