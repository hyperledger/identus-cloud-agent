package io.iohk.atala.issue.controller

import io.iohk.atala.api.http.EndpointOutputs.*
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.iam.authentication.oidc.JwtSecurityLogic.bearerAuthHeader
import io.iohk.atala.issue.controller.http.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody

object IssueEndpoints {

  private val paginationInput: EndpointInput[PaginationInput] = EndpointInput.derived[PaginationInput]

  val createCredentialOffer: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, CreateIssueCredentialRecordRequest),
    ErrorResponse,
    IssueCredentialRecord,
    Any
  ] =
    endpoint.post
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("issue-credentials" / "credential-offers")
      .in(jsonBody[CreateIssueCredentialRecordRequest].description("The credential offer object."))
      .errorOut(basicFailuresWith(FailureVariant.forbidden, FailureVariant.notFound, FailureVariant.badRequest))
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[IssueCredentialRecord].description("The issue credential record."))
      .tag("Issue Credentials Protocol")
      .summary("As a credential issuer, create a new credential offer to be sent to a holder.")
      .description("Creates a new credential offer in the database")
      .name("createCredentialOffer")

  val getCredentialRecords: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, PaginationInput, Option[String]),
    ErrorResponse,
    IssueCredentialRecordPage,
    Any
  ] =
    endpoint.get
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in("issue-credentials" / "records")
      .in(paginationInput)
      .in(query[Option[String]]("thid").description("The thid of a DIDComm communication."))
      .errorOut(basicFailuresAndForbidden)
      .out(jsonBody[IssueCredentialRecordPage].description("The list of issue credential records."))
      .tag("Issue Credentials Protocol")
      .summary("Gets the list of issue credential records.")
      .description("Get the list of issue credential records paginated")
      .name("getCredentialRecords")

  val getCredentialRecord: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, String),
    ErrorResponse,
    IssueCredentialRecord,
    Any
  ] =
    endpoint.get
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "issue-credentials" / "records" / path[String]("recordId").description(
          "The unique identifier of the issue credential record."
        )
      )
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .out(jsonBody[IssueCredentialRecord].description("The issue credential record."))
      .tag("Issue Credentials Protocol")
      .summary("Gets an existing issue credential record by its unique identifier.")
      .description("Gets issue credential records by record id")
      .name("getCredentialRecord")

  val acceptCredentialOffer: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, String, AcceptCredentialOfferRequest),
    ErrorResponse,
    IssueCredentialRecord,
    Any
  ] =
    endpoint.post
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "issue-credentials" / "records" / path[String]("recordId").description(
          "The unique identifier of the issue credential record."
        )
      )
      .in("accept-offer")
      .in(jsonBody[AcceptCredentialOfferRequest].description("The accept credential offer request object."))
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .out(jsonBody[IssueCredentialRecord].description("The issue credential offer was successfully accepted."))
      .tag("Issue Credentials Protocol")
      .summary("As a holder, accepts a credential offer received from an issuer.")
      .description("Accepts a credential offer received from a VC issuer and sends back a credential request.")
      .name("acceptCredentialOffer")

  val issueCredential: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, String),
    ErrorResponse,
    IssueCredentialRecord,
    Any
  ] =
    endpoint.post
      .securityIn(apiKeyHeader)
      .securityIn(bearerAuthHeader)
      .in(extractFromRequest[RequestContext](RequestContext.apply))
      .in(
        "issue-credentials" / "records" / path[String]("recordId").description(
          "The unique identifier of the issue credential record."
        )
      )
      .in("issue-credential")
      .errorOut(basicFailureAndNotFoundAndForbidden)
      .out(
        jsonBody[IssueCredentialRecord].description(
          "The request was processed successfully and the credential will be issued asynchronously."
        )
      )
      .tag("Issue Credentials Protocol")
      .summary("As an issuer, issues the verifiable credential related to the specified record.")
      .description(
        "Sends credential to a holder (holder DID is specified in credential as subjectDid). Credential is constructed from the credential records found by credential id."
      )
      .name("issueCredential")

}
