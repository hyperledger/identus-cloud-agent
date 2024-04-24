package org.hyperledger.identus.presentproof.controller

import org.hyperledger.identus.api.http.EndpointOutputs.*
import org.hyperledger.identus.api.http.model.PaginationInput
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.iam.authentication.apikey.ApiKeyCredentials
import org.hyperledger.identus.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import org.hyperledger.identus.iam.authentication.oidc.JwtCredentials
import org.hyperledger.identus.iam.authentication.oidc.JwtSecurityLogic.jwtAuthHeader
import org.hyperledger.identus.presentproof.controller.http.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

import java.util.UUID

object PresentProofEndpoints {

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val requestPresentation: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, RequestPresentationInput),
    ErrorResponse,
    PresentationStatus,
    Any
  ] =
    endpoint.post
      .tag("Present Proof")
      .name("requestPresentation")
      .summary("As a Verifier, create a new proof presentation request and send it to the Prover.")
      .description("Holder presents proof derived from the verifiable credential to verifier.")
      .securityIn(apiKeyHeader)
      .securityIn(jwtAuthHeader)
      .in("present-proof" / "presentations")
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(jsonBody[RequestPresentationInput].description("The present proof creation request."))
      .out(
        statusCode(StatusCode.Created).description(
          "The proof presentation request was created successfully and will be sent asynchronously to the Prover."
        )
      )
      .out(jsonBody[PresentationStatus])
      .errorOut(basicFailureAndNotFoundAndForbidden)

  val getAllPresentations: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, PaginationInput, Option[String]),
    ErrorResponse,
    PresentationStatusPage,
    Any
  ] =
    endpoint.get
      .tag("Present Proof")
      .name("getAllPresentation")
      .summary("Gets the list of proof presentation records.")
      .description("list of presentation statuses")
      .securityIn(apiKeyHeader)
      .securityIn(jwtAuthHeader)
      .in("present-proof" / "presentations")
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(paginationInput)
      .in(query[Option[String]]("thid"))
      .out(statusCode(StatusCode.Ok).description("The list of proof presentation records."))
      .out(jsonBody[PresentationStatusPage])
      .errorOut(basicFailuresAndForbidden)

  val getPresentation
      : Endpoint[(ApiKeyCredentials, JwtCredentials), (RequestContext, UUID), ErrorResponse, PresentationStatus, Any] =
    endpoint.get
      .tag("Present Proof")
      .name("getPresentation")
      .summary(
        "Gets an existing proof presentation record by its unique identifier. " +
          "More information on the error can be found in the response body."
      )
      .description("Returns an existing presentation record by id.")
      .securityIn(apiKeyHeader)
      .securityIn(jwtAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "present-proof" / "presentations" / path[UUID]("presentationId").description(
          "The unique identifier of the presentation record."
        )
      )
      .out(statusCode(StatusCode.Ok).description("The proof presentation record."))
      .out(jsonBody[PresentationStatus])
      .errorOut(basicFailureAndNotFoundAndForbidden)

  val updatePresentation: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, UUID, RequestPresentationAction),
    ErrorResponse,
    PresentationStatus,
    Any
  ] =
    endpoint.patch
      .tag("Present Proof")
      .name("updatePresentation")
      .summary(
        "Updates the proof presentation record matching the unique identifier, " +
          "with the specific action to perform."
      )
      .description("Accept or reject presentation of proof request.")
      .securityIn(apiKeyHeader)
      .securityIn(jwtAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "present-proof" / "presentations" / path[UUID]("presentationId").description(
          "The unique identifier of the presentation record."
        )
      )
      .in(jsonBody[RequestPresentationAction].description("The action to perform on the proof presentation record."))
      .out(statusCode(StatusCode.Ok).description("The proof presentation record was successfully updated."))
      .out(jsonBody[PresentationStatus])
      .errorOut(basicFailureAndNotFoundAndForbidden)

}
