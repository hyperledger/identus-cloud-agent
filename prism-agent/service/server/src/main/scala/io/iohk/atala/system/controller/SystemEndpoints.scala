package io.iohk.atala.system.controller

import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.connect.controller.http.{Connection, CreateConnectionRequest}
import io.iohk.atala.issue.controller.http.*
import io.iohk.atala.system.controller.http.HealthInfo
import sttp.model.StatusCode
import sttp.tapir.ztapir.stringBody
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.{
  Endpoint,
  EndpointInfo,
  EndpointInput,
  PublicEndpoint,
  endpoint,
  extractFromRequest,
  oneOf,
  oneOfDefaultVariant,
  oneOfVariant,
  path,
  query,
  statusCode,
  stringToPath
}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

import java.util.UUID

object SystemEndpoints {

  val health: PublicEndpoint[
    (RequestContext),
    ErrorResponse,
    HealthInfo,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("_system" / "health")
      .out(jsonBody[HealthInfo].description("The health info object."))
      .errorOut(basicFailures)
      .tag("System")
      .summary("As a system user, check the health status of the running service")
      .description("Returns the health info object of the running service")
      .name("systemHealth")

  val metrics: PublicEndpoint[
    (RequestContext),
    ErrorResponse,
    String,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("_system" / "metrics")
      .out(stringBody.description("The metrics as pain strings."))
      .errorOut(basicFailures)
      .tag("System")
      .summary("As a system user, check the health status of the running service")
      .description("Returns the health info object of the running service")
      .name("systemMetrics")

}
