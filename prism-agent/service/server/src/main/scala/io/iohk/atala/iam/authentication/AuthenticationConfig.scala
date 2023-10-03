package io.iohk.atala.iam.authentication

import io.iohk.atala.iam.authentication.admin.AdminConfig
import io.iohk.atala.iam.authentication.apikey.ApiKeyConfig
import zio.config.*
import zio.config.magnolia.*

import scala.util.Try

final case class AuthenticationConfig(method: AuthMethod, admin: AdminConfig, apiKey: ApiKeyConfig)

enum AuthMethod {
  case none, apiKey, keycloak
}

object AuthMethod {
  given Descriptor[AuthMethod] =
    Descriptor.from(
      Descriptor[String].transformOrFailLeft { s =>
        Try(AuthMethod.valueOf(s)).toOption
          .toRight(s"Invalid configuration value '$s'. Possible values: ${AuthMethod.values.mkString("[", ", ", "]")}")
      }(_.toString())
    )
}
