package io.iohk.atala.presentproof.controller

import io.iohk.atala.api.http.EndpointOutputs.{basicFailures, basicFailuresAndNotFound}
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.presentproof.controller.http.*
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.{EndpointInput, PublicEndpoint, endpoint, extractFromRequest, path, query, statusCode, stringToPath}

import java.util.UUID

object PresentProofEndpoints {

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val requestPresentation: PublicEndpoint[
    (RequestContext, RequestPresentationInput),
    ErrorResponse,
    RequestPresentationOutput,
    Any
  ] =
    endpoint.post
      .tag("Present Proof")
      .name("requestPresentation")
      .summary("As a Verifier, create a new proof presentation request and send it to the Prover.")
      .description("Holder presents proof derived from the verifiable credential to verifier.")
      .in("present-proof" / "presentations")
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(jsonBody[RequestPresentationInput].description("The present proof creation request."))
      .out(
        statusCode(StatusCode.Created).description(
          "The proof presentation request was created successfully and will be sent asynchronously to the Prover."
        )
      )
      .out(jsonBody[RequestPresentationOutput])
      .errorOut(basicFailures)

  val getAllPresentations
      : PublicEndpoint[(RequestContext, PaginationInput, Option[String]), ErrorResponse, PresentationStatusPage, Any] =
    endpoint.get
      .tag("Present Proof")
      .name("getAllPresentation")
      .summary("Gets the list of proof presentation records.")
      .description("list of presentation statuses")
      .in("present-proof" / "presentations")
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(paginationInput)
      .in(query[Option[String]]("thid"))
      .out(statusCode(StatusCode.Ok).description("The list of proof presentation records."))
      .out(jsonBody[PresentationStatusPage])
      .errorOut(basicFailures)

  val getPresentation: PublicEndpoint[(RequestContext, UUID), ErrorResponse, PresentationStatus, Any] =
    endpoint.get
      .tag("Present Proof")
      .name("getPresentation")
      .summary(
        "Gets an existing proof presentation record by its unique identifier. " +
          "More information on the error can be found in the response body."
      )
      .description("Returns an existing presentation record by id.")
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "present-proof" / "presentations" / path[UUID]("presentationId").description(
          "The unique identifier of the presentation record."
        )
      )
      .out(statusCode(StatusCode.Ok).description("The proof presentation record."))
      .out(jsonBody[PresentationStatus])
      .errorOut(basicFailuresAndNotFound)

  val updatePresentation: PublicEndpoint[
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
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "present-proof" / "presentations" / path[UUID]("presentationId").description(
          "The unique identifier of the presentation record."
        )
      )
      .in(jsonBody[RequestPresentationAction].description("The action to perform on the proof presentation record."))
      .out(statusCode(StatusCode.Ok).description("The proof presentation record was successfully updated."))
      .out(jsonBody[PresentationStatus])
      .errorOut(basicFailuresAndNotFound)

}
