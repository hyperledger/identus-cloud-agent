package io.iohk.atala.connect.controller

import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.connect.controller.http.{Connection, CreateConnectionRequest}
import sttp.model.StatusCode
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

object ConnectionEndpoints {

  val createConnection: PublicEndpoint[
    (RequestContext, CreateConnectionRequest),
    ErrorResponse,
    Connection,
    Any
  ] =
    endpoint.post
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
      .description("Credential schema record")
      .errorOut(basicFailures)
      .name("createConnection")
      .summary("Creates a new connection record and returns an Out of Band invitation.")
      .description("""
         |Generates a new Peer DID and creates an [Out of Band 2.0](https://identity.foundation/didcomm-messaging/spec/v2.0/#out-of-band-messages) invitation.
         |It returns a new connection record in `InvitationGenerated` state.
         |The request body may contain a `label` that can be used as a human readable alias for the connection, for example `{'label': "Bob"}`
         |""".stripMargin)
      .tag("Connections Management")

}
