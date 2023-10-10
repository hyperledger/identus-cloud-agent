package io.iohk.atala.iam.authentication.admin

import sttp.tapir.EndpointIO
import sttp.tapir.ztapir.*

object AdminApiKeySecurityLogic {
  val adminApiKeyHeader: EndpointIO.Header[AdminApiKeyCredentials] = header[String]("x-admin-api-key")
    .mapTo[AdminApiKeyCredentials]
    .description("Admin API Key")
}
