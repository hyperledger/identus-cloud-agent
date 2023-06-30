package io.iohk.atala.pollux.credentialschema

import io.iohk.atala.api.http.*
import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.api.http.codec.OrderCodec.*
import io.iohk.atala.api.http.model.{Order, PaginationInput}
import io.iohk.atala.pollux.credentialschema.http.{
  CredentialSchemaInput,
  CredentialSchemaResponse,
  CredentialSchemaResponsePage,
  FilterInput
}
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.{
  EndpointInput,
  PublicEndpoint,
  endpoint,
  extractFromRequest,
  path,
  query,
  statusCode,
  stringToPath
}
import java.util.UUID

object SchemaRegistryEndpoints {

  val createSchemaEndpoint: PublicEndpoint[
    (RequestContext, CredentialSchemaInput),
    ErrorResponse,
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
      .errorOut(basicFailuresAndNotFound)
      .name("createSchema")
      .summary("Publish new schema to the schema registry")
      .description(
        "Create the new credential schema record with metadata and internal JSON Schema on behalf of Cloud Agent. " +
          "The credential schema will be signed by the keys of Cloud Agent and issued by the DID that corresponds to it."
      )
      .tag("Schema Registry")

  val updateSchemaEndpoint: PublicEndpoint[
    (RequestContext, String, UUID, CredentialSchemaInput),
    ErrorResponse,
    CredentialSchemaResponse,
    Any
  ] =
    endpoint.put
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "schema-registry" /
          path[String]("author").description(CredentialSchemaResponse.annotations.author.description) /
          path[UUID]("id").description(CredentialSchemaResponse.annotations.id.description)
      )
      .in(
        jsonBody[CredentialSchemaInput]
          .description(
            "JSON object required for the credential schema update"
          )
      )
      .out(
        statusCode(StatusCode.Ok)
          .description(
            "The credential schema record is successfully updated"
          )
      )
      .out(jsonBody[CredentialSchemaResponse])
      .description("Credential schema record")
      .errorOut(basicFailuresAndNotFound)
      .name("updateSchema")
      .summary("Publish the new version of the credential schema to the schema registry")
      .description(
        "Publish the new version of the credential schema record with metadata and internal JSON Schema on behalf of Cloud Agent. " +
          "The credential schema will be signed by the keys of Cloud Agent and issued by the DID that corresponds to it."
      )
      .tag("Schema Registry")

  val getSchemaByIdEndpoint: PublicEndpoint[
    (RequestContext, UUID),
    ErrorResponse,
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
      .out(jsonBody[CredentialSchemaResponse].description("CredentialSchema found by `guid`"))
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
    ErrorResponse,
    CredentialSchemaResponsePage,
    Any
  ] =
    endpoint.get
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("schema-registry" / "schemas".description("Lookup schemas by query"))
      .in(credentialSchemaFilterInput)
      .in(paginationInput)
      .in(query[Option[Order]]("order"))
      .out(jsonBody[CredentialSchemaResponsePage].description("Collection of CredentialSchema records."))
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
