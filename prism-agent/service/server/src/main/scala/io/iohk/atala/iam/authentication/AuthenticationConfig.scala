package io.iohk.atala.iam.authentication

import io.iohk.atala.iam.authentication.admin.AdminConfig
import io.iohk.atala.iam.authentication.apikey.ApiKeyConfig

final case class AuthenticationConfig(admin: AdminConfig, apiKey: ApiKeyConfig)
