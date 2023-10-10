package io.iohk.atala.connect.controller

import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.connect.controller.http.{
  AcceptConnectionInvitationRequest,
  Connection,
  ConnectionsPage,
  CreateConnectionRequest
}
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.iam.authentication.oidc.JwtSecurityLogic.bearerAuthHeader
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

import java.util.UUID

object ConnectionEndpoints {

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val createConnection: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, CreateConnectionRequest),
    ErrorResponse,
    Connection,
    Any
  ] =
    endpoint.post
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("connections")
      .in(
        jsonBody[CreateConnectionRequest].description(
          "JSON object required for the connection creation"
        )
      )
      .out(
        statusCode(StatusCode.Created)
          .description(
            "The connection record was created successfully, and is returned in the response body."
          )
      )
      .out(jsonBody[Connection])
      .description("The created connection record.")
      .errorOut(basicFailuresAndForbidden)
      .name("createConnection")
      .summary("Creates a new connection record and returns an Out of Band invitation.")
      .description("""
         |Generates a new Peer DID and creates an [Out of Band 2.0](https://identity.foundation/didcomm-messaging/spec/v2.0/#out-of-band-messages) invitation.
         |It returns a new connection record in `InvitationGenerated` state.
         |The request body may contain a `label` that can be used as a human readable alias for the connection, for example `{'label': "Bob"}`
         |""".stripMargin)
      .tag("Connections Management")

  val getConnection
      : Endpoint[(ApiKeyCredentials, JwtCredentials), (RequestContext, UUID), ErrorResponse, Connection, Any] =
    endpoint.get
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "connections" / path[UUID]("connectionId").description(
          "The unique identifier of the connection record."
        )
      )
      .out(jsonBody[Connection].description("The connection record."))
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .name("getConnection")
      .summary("Gets an existing connection record by its unique identifier.")
      .description("Gets an existing connection record by its unique identifier")
      .tag("Connections Management")

  val getConnections: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, PaginationInput, Option[String]),
    ErrorResponse,
    ConnectionsPage,
    Any
  ] =
    endpoint.get
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("connections")
      .in(paginationInput)
      .in(query[Option[String]]("thid").description("The thid of a DIDComm communication."))
      .out(jsonBody[ConnectionsPage].description("The list of connection records."))
      .errorOut(basicFailuresAndForbidden)
      .name("getConnections")
      .summary("Gets the list of connection records.")
      .description("Get the list of connection records paginated")
      .tag("Connections Management")

  val acceptConnectionInvitation: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, AcceptConnectionInvitationRequest),
    ErrorResponse,
    Connection,
    Any
  ] =
    endpoint.post
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("connection-invitations")
      .in(
        jsonBody[AcceptConnectionInvitationRequest].description(
          "The request used by an invitee to accept a connection invitation received from an inviter, using out-of-band mechanism."
        )
      )
      .out(
        statusCode(StatusCode.Ok)
          .description(
            "The invitation was successfully accepted."
          )
      )
      .out(jsonBody[Connection])
      .description("The created connection record.")
      .errorOut(basicFailuresAndForbidden)
      .name("acceptConnectionInvitation")
      .summary("Accepts an Out of Band invitation.")
      .description("""
          |Accepts an [Out of Band 2.0](https://identity.foundation/didcomm-messaging/spec/v2.0/#out-of-band-messages) invitation, generates a new Peer DID,
          |and submits a Connection Request to the inviter.
          |It returns a connection object in `ConnectionRequestPending` state, until the Connection Request is eventually sent to the inviter by the prism-agent's background process. The connection object state will then automatically move to `ConnectionRequestSent`.
          |""".stripMargin)
      .tag("Connections Management")

}
