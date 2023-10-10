package io.iohk.atala.event.controller

import io.iohk.atala.api.http.EndpointOutputs
import io.iohk.atala.api.http.EndpointOutputs.FailureVariant
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.event.controller.http.CreateWebhookNotification
import io.iohk.atala.event.controller.http.WebhookNotification
import io.iohk.atala.event.controller.http.WebhookNotificationPage
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.iam.authentication.oidc.JwtSecurityLogic.bearerAuthHeader
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

import java.util.UUID

object EventEndpoints {

  private val baseEndpoint = endpoint
    .tag("Events")
    .in("events")
    .securityIn(apiKeyHeader)
    .securityIn(bearerAuthHeader)
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  val createWebhookNotification: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, CreateWebhookNotification),
    ErrorResponse,
    WebhookNotification,
    Any
  ] = baseEndpoint.post
    .in("webhooks")
    .in(jsonBody[CreateWebhookNotification])
    .errorOut(EndpointOutputs.basicFailuresWith(FailureVariant.forbidden, FailureVariant.conflict))
    .out(statusCode(StatusCode.Ok).description("Webhook notification has been created successfully"))
    .out(jsonBody[WebhookNotification])
    .summary("Create wallet webhook notifications")

  val listWebhookNotification: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    RequestContext,
    ErrorResponse,
    WebhookNotificationPage,
    Any
  ] = baseEndpoint.get
    .in("webhooks")
    .errorOut(EndpointOutputs.basicFailuresAndForbidden)
    .out(statusCode(StatusCode.Ok).description("List wallet webhook notifications"))
    .out(jsonBody[WebhookNotificationPage])
    .summary("List wallet webhook notifications")

  val deleteWebhookNotification: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, UUID),
    ErrorResponse,
    Unit,
    Any
  ] = baseEndpoint.delete
    .in("webhooks" / path[UUID]("id").description("ID of the webhook notification to delete."))
    .errorOut(EndpointOutputs.basicFailureAndNotFoundAndForbidden)
    .out(statusCode(StatusCode.Ok).description("Webhook notification has been deleted."))
    .summary("Delete the wallet webhook notification by `id`")

}
