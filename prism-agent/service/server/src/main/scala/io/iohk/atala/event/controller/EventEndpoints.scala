package io.iohk.atala.event.controller

import io.iohk.atala.api.http.EndpointOutputs
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.event.controller.http.CreateWebhookNotification
import io.iohk.atala.event.controller.http.WebhookNotification
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

object EventEndpoints {

  private val baseEndpoint = endpoint
    .tag("Events")
    .in("events")
    .securityIn(apiKeyHeader)
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  val createWebhookNotification: Endpoint[
    ApiKeyCredentials,
    (RequestContext, CreateWebhookNotification),
    ErrorResponse,
    WebhookNotification,
    Any
  ] = baseEndpoint.post
    .in("webhooks")
    .in(jsonBody[CreateWebhookNotification])
    .errorOut(EndpointOutputs.basicFailuresAndForbidden)
    .out(statusCode(StatusCode.Ok).description("Webhook notification has been created successfully"))
    .out(jsonBody[WebhookNotification])

}
