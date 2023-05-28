package io.iohk.atala.pollux.api

import io.iohk.atala.pollux.models.{
  BadRequest,
  ErrorResponse,
  FailureResponse,
  InternalServerError,
  NotFoundResponse,
  UnauthorizedResponse,
  UnknownResponse,
  VerifiableCredentialsSchema,
  VerifiableCredentialsSchemaInput
}
import sttp.tapir.EndpointIO.Info
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.{Endpoint, EndpointInfo, PublicEndpoint, endpoint, oneOf, oneOfDefaultVariant, oneOfVariant, path, stringToPath}
import sttp.tapir.generic.auto.*
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}
import sttp.model.StatusCode
import sttp.tapir.statusCode

import java.util.UUID

object VerifiableCredentialsSchemaEndpoints {

  val createSchemaEndpoint: PublicEndpoint[VerifiableCredentialsSchemaInput, Unit, VerifiableCredentialsSchema, Any] =
    endpoint.post
      .in("schema-registry" / "schemas")
      .in(
        jsonBody[VerifiableCredentialsSchemaInput]
          .copy(info = Info.empty.description("Create schema input object with the metadata and attributes"))
      )
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[VerifiableCredentialsSchema])
      .name("createSchema")
      .summary("Publish new schema to the schema registry")
      .description(
        "Publish the new schema with attributes to the schema registry on behalf of Cloud Agent. Schema will be signed by the keys of Cloud Agent and issued by the DID that corresponds to it"
      )
      .tag("Schema Registry")

  val getSchemaById: PublicEndpoint[UUID, FailureResponse, VerifiableCredentialsSchema, Any] =
    endpoint.get
      .in(
        "schema-registry" / "schemas" / path[UUID]("id")
          .copy(info = Info.empty.description("Schema Id"))
      )
      .out(jsonBody[VerifiableCredentialsSchema])
      .errorOut(
        oneOf[FailureResponse](
          oneOfVariant(StatusCode.NotFound, jsonBody[NotFoundResponse])
        )
      )
      .name("getSchemaById")
      .summary("Fetch the schema from the registry by id")
      .description("Fetch the schema by the unique identifier. Verifiable Credential Schema in json format is returned.")
      .tag("Schema Registry")
}
