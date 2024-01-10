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
import io.iohk.atala.iam.authentication.oidc.JwtSecurityLogic.jwtAuthHeader
import sttp.apispec.Tag
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

import java.util.UUID

object ConnectionEndpoints {

  private val tagName = "Connections Management"
  private val tagDescription =
    s"""
       |The '$tagName' endpoints facilitate the initiation of connection flows between the current agent and peer agents, regardless of whether they reside in cloud or edge environments.
       |<br>
       |This implementation adheres to the DIDComm Messaging v2.0 - [Out of Band Messages](https://identity.foundation/didcomm-messaging/spec/v2.0/#out-of-band-messages) specification [section 9.5.4](https://identity.foundation/didcomm-messaging/spec/v2.0/#invitation) - to generate invitations.
       |The <b>from</b> field of the out-of-band invitation message contains a freshly generated Peer DID that complies with the [did:peer:2](https://identity.foundation/peer-did-method-spec/#generating-a-didpeer2) specification.
       |This Peer DID includes the 'uri' location of the DIDComm messaging service, essential for the invitee's subsequent execution of the connection flow.
       |<br>
       |Upon accepting an invitation, the invitee sends a connection request to the inviter's DIDComm messaging service endpoint.
       |The connection request's 'type' attribute must be specified as "https://atalaprism.io/mercury/connections/1.0/request".
       |The inviter agent responds with a connection response message, indicated by a 'type' attribute of "https://atalaprism.io/mercury/connections/1.0/response".
       |Both request and response types are proprietary to the Open Enterprise Agent ecosystem.
       |""".stripMargin

  val tag = Tag(tagName, Some(tagDescription))

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
      .securityIn(jwtAuthHeader)
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
      .description("The newly created connection record.")
      .errorOut(basicFailuresAndForbidden)
      .name("createConnection")
      .summary("Create a new connection invitation that can be delivered out-of-band to a peer agent.")
      .description("""
         |Create a new connection invitation that can be delivered out-of-band to a peer agent, regardless of whether it resides in cloud or edge environment.
         |The generated invitation adheres to the DIDComm Messaging v2.0 - [Out of Band Messages](https://identity.foundation/didcomm-messaging/spec/v2.0/#out-of-band-messages) specification [section 9.5.4](https://identity.foundation/didcomm-messaging/spec/v2.0/#invitation).
         |The <b>from</b> field of the out-of-band invitation message contains a freshly generated Peer DID that complies with the [did:peer:2](https://identity.foundation/peer-did-method-spec/#generating-a-didpeer2) specification.
         |This Peer DID includes the 'uri' location of the DIDComm messaging service, essential for the invitee's subsequent execution of the connection flow.
         |In the agent database, the created connection record has an initial state set to `InvitationGenerated`.
         |The request body may contain a `label` that can be used as a human readable alias for the connection, for example `{'label': "Connection with Bob"}`
         |""".stripMargin)
      .tag(tagName)

  val getConnection
      : Endpoint[(ApiKeyCredentials, JwtCredentials), (RequestContext, UUID), ErrorResponse, Connection, Any] =
    endpoint.get
      .securityIn(apiKeyHeader)
      .securityIn(jwtAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "connections" / path[UUID]("connectionId").description(
          "The `connectionId` uniquely identifying the connection flow record."
        )
      )
      .out(jsonBody[Connection].description("The specific connection flow record."))
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .name("getConnection")
      .summary(
        "Retrieves a specific connection flow record from the agent's database based on its unique `connectionId`."
      )
      .description("""
          |Retrieve a specific connection flow record from the agent's database based in its unique `connectionId`.
          |The API returns a comprehensive collection of connection flow records within the system, regardless of their state.
          |The returned connection item includes essential metadata such as connection ID, thread ID, state, role, participant information, and other relevant details.
          |""".stripMargin)
      .tag(tagName)

  val getConnections: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, PaginationInput, Option[String]),
    ErrorResponse,
    ConnectionsPage,
    Any
  ] =
    endpoint.get
      .securityIn(apiKeyHeader)
      .securityIn(jwtAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("connections")
      .in(paginationInput)
      .in(
        query[Option[String]]("thid").description(
          "The `thid`, shared between the inviter and the invitee, that uniquely identifies a connection flow."
        )
      )
      .out(
        jsonBody[ConnectionsPage].description("The list of connection flow records available from the agent's database")
      )
      .errorOut(basicFailuresAndForbidden)
      .name("getConnections")
      .summary("Retrieves the list of connection flow records available from the agent's database.")
      .description("""
          |Retrieve of a list containing connections available from the agent's database.
          |The API returns a comprehensive collection of connection flow records within the system, regardless of their state.
          |Each connection item includes essential metadata such as connection ID, thread ID, state, role, participant information, and other relevant details.
          |Pagination support is available, allowing for efficient handling of large datasets.
          |""".stripMargin)
      .tag(tagName)

  val acceptConnectionInvitation: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, AcceptConnectionInvitationRequest),
    ErrorResponse,
    Connection,
    Any
  ] =
    endpoint.post
      .securityIn(apiKeyHeader)
      .securityIn(jwtAuthHeader)
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
      .description("The newly connection record.")
      .errorOut(basicFailuresAndForbidden)
      .name("acceptConnectionInvitation")
      .summary("Accept a new connection invitation received out-of-band from another peer agent.")
      .description("""
          |Accept an new connection invitation received out-of-band from another peer agent.
          |The invitation must be compliant with the DIDComm Messaging v2.0 - [Out of Band Messages](https://identity.foundation/didcomm-messaging/spec/v2.0/#out-of-band-messages) specification [section 9.5.4](https://identity.foundation/didcomm-messaging/spec/v2.0/#invitation).
          |A new connection record with state `ConnectionRequestPending` will be created in the agent database and later processed by a background job to send a connection request to the peer agent.
          |The created record will contain a newly generated pairwise Peer DID used for that connection.
          |A connection request will then be sent to the peer agent to actually establish the connection, moving the record state to `ConnectionRequestSent`, and waiting the connection response from the peer agent.
          |""".stripMargin)
      .tag(tagName)

}
