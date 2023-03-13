package io.iohk.atala.pollux.credentialschema

import io.iohk.atala.api.http.*
import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.api.http.codec.OrderCodec.*
import io.iohk.atala.api.http.model.{Order, PaginationInput}
import io.iohk.atala.api.http.*
import io.iohk.atala.pollux.credentialschema.http.{
  CredentialSchemaInput,
  CredentialSchemaPageResponse,
  CredentialSchemaResponse,
  FilterInput
}
import sttp.model.StatusCode
import sttp.tapir.EndpointIO.Info
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

object SchemaRegistryEndpoints {

  val createSchemaEndpoint: PublicEndpoint[
    (RequestContext, CredentialSchemaInput),
    FailureResponse,
    CredentialSchemaResponse,
    Any
  ] =
    endpoint.post
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("schema-registry" / "schemas")
      .in(
        jsonBody[CredentialSchemaInput]
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
      .out(jsonBody[CredentialSchemaResponse])
      .description("Credential schema record")
      .errorOut(basicFailures)
      .name("createSchema")
      .summary("Publish new schema to the schema registry")
      .description(
        "Create the new credential schema record with metadata and internal JSON Schema on behalf of Cloud Agent.<br/>" +
          "The credential schema will be signed by the keys of Cloud Agent and issued by the DID that corresponds to it"
      )
      .tag("Schema Registry")

  val getSchemaByIdEndpoint: PublicEndpoint[
    (RequestContext, UUID),
    FailureResponse,
    CredentialSchemaResponse,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "schema-registry" / "schemas" / path[UUID]("guid").description(
          "Globally unique identifier of the credential schema record"
        )
      )
      .out(jsonBody[CredentialSchemaResponse])
      .errorOut(basicFailuresAndNotFound)
      .name("getSchemaById")
      .summary("Fetch the schema from the registry by `guid`")
      .description(
        "Fetch the credential schema by the unique identifier"
      )
      .tag("Schema Registry")

  private val credentialSchemaFilterInput: EndpointInput[FilterInput] = EndpointInput.derived[FilterInput]
  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]
  val lookupSchemasByQueryEndpoint: PublicEndpoint[
    (
        RequestContext,
        FilterInput,
        PaginationInput,
        Option[Order]
    ),
    FailureResponse,
    CredentialSchemaPageResponse,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("schema-registry" / "schemas".description("Lookup schemas by query"))
      .in(credentialSchemaFilterInput)
      .in(paginationInput)
      .in(query[Option[Order]]("order"))
      .out(jsonBody[CredentialSchemaPageResponse])
      .errorOut(basicFailures)
      .name("lookupSchemasByQuery")
      .summary("Lookup schemas by indexed fields")
      .description(
        "Lookup schemas by `author`, `name`, `tags` parameters and control the pagination by `offset` and `limit` parameters "
      )
      .tag("Schema Registry")

  val testEndpoint: PublicEndpoint[
    RequestContext,
    Unit,
    String,
    Any
  ] =
    endpoint.get
      .in(
        "schema-registry" / "test"
          .description("Debug endpoint")
      )
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .out(jsonBody[String])
      .name("test")
      .summary("Trace the request input from the point of view of the server")
      .description(
        "Trace the request input from the point of view of the server"
      )
      .tag("Schema Registry")

}
