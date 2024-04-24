package org.hyperledger.identus.event.controller

import org.hyperledger.identus.api.http.EndpointOutputs
import org.hyperledger.identus.api.http.EndpointOutputs.FailureVariant
import org.hyperledger.identus.api.http.ErrorResponse
import org.hyperledger.identus.api.http.RequestContext
import org.hyperledger.identus.event.controller.http.CreateWebhookNotification
import org.hyperledger.identus.event.controller.http.WebhookNotification
import org.hyperledger.identus.event.controller.http.WebhookNotificationPage
import org.hyperledger.identus.iam.authentication.apikey.ApiKeyCredentials
import org.hyperledger.identus.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import org.hyperledger.identus.iam.authentication.oidc.JwtCredentials
import org.hyperledger.identus.iam.authentication.oidc.JwtSecurityLogic.jwtAuthHeader
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

import java.util.UUID

object EventEndpoints {

  private val baseEndpoint = endpoint
    .tag("Events")
    .in("events")
    .securityIn(apiKeyHeader)
    .securityIn(jwtAuthHeader)
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
