package org.hyperledger.identus.iam.authentication.oidc

import org.hyperledger.identus.iam.authentication.AuthenticationError
import org.hyperledger.identus.iam.authentication.Credentials

final case class JwtCredentials(token: Option[String]) extends Credentials

final case class JwtAuthenticationError(message: String) extends AuthenticationError

object JwtAuthenticationError {
  val emptyToken = JwtAuthenticationError("Empty bearer token header provided")
}
