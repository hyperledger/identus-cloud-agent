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
      .in("" / "connections")
      .in(
        jsonBody[CreateConnectionRequest]
          .description(
            "JSON object required for the credential schema creation"
          )
      )
      .out(
        statusCode(StatusCode.Created)
          .description(
            "The new credential schema record is successfully created"
          )
      )
      .out(jsonBody[Connection])
      .description("Credential schema record")
      .errorOut(basicFailures)
      .name("createSchema")
      .summary("Publish new schema to the schema registry")
      .description(
        "Create the new credential schema record with metadata and internal JSON Schema on behalf of Cloud Agent. " +
          "The credential schema will be signed by the keys of Cloud Agent and issued by the DID that corresponds to it."
      )
      .tag("Schema Registry")

}
