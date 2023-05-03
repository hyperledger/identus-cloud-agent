package io.iohk.atala.issue.controller

import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.issue.controller.IssueController
import io.iohk.atala.pollux.core.model.DidCommID
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.vc.jwt.W3cCredentialPayload
import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.pollux.vc.jwt.CredentialPayload.Implicits.*
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*

import java.security.*
import java.security.spec.*
import java.time.{Instant, ZonedDateTime}

object IssueControllerSpec extends ZIOSpecDefault {

  override def spec = suite("IssueControllerSpec")(httpErrorSpec)

  private val httpErrorSpec = suite("testHttpErrors")(
    test("return internal server error if repository error") {
      val cse = CredentialServiceError.RepositoryError(new Throwable("test throw"))
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.internalServerError(title = "RepositoryError", detail = Some(cse.cause.toString))
      assert(httpError)(equalTo(errorResponse))
    },
    test("return not found error if record id not found") {
      val cse = CredentialServiceError.RecordIdNotFound(DidCommID("12345"))
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.notFound(detail = Some(s"Record Id not found: 12345"))
      assert(httpError)(equalTo(errorResponse))
    },
    test("return internal server error if operation not executed") {
      val cse = CredentialServiceError.OperationNotExecuted(DidCommID("12345"), "info")
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.internalServerError(title = "Operation Not Executed", detail = Some(s"12345-info"))
      assert(httpError)(equalTo(errorResponse))
    },
    test("return not found error if thread Id not found") {
      val cse = CredentialServiceError.ThreadIdNotFound(DidCommID("12345"))
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.notFound(detail = Some(s"Thread Id not found: 12345"))
      assert(httpError)(equalTo(errorResponse))
    },
    test("return internal server error if unexpected error") {
      val cse = CredentialServiceError.UnexpectedError("message")
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.internalServerError(detail = Some("message"))
      assert(httpError)(equalTo(errorResponse))
    },
    test("return bad request error if invalid flow state error") {
      val cse = CredentialServiceError.InvalidFlowStateError("message")
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.badRequest(title = "InvalidFlowState", detail = Some("message"))
      assert(httpError)(equalTo(errorResponse))
    },
    test("return bad request error if unsupported did format error") {
      val cse = CredentialServiceError.UnsupportedDidFormat("12345")
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.badRequest("Unsupported DID format", Some(s"The following DID is not supported: 12345"))
      assert(httpError)(equalTo(errorResponse))
    },
    test("return bad request error if create credential payload from record error") {
      val cse = CredentialServiceError.CreateCredentialPayloadFromRecordError(new Throwable("message"))
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.badRequest(title = "Create Credential Payload From Record Error", detail = Some(cse.cause.getMessage))
      assert(httpError)(equalTo(errorResponse))
    },
    test("return bad request error if create request validation error") {
      val cse = CredentialServiceError.CredentialRequestValidationError("message")
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.badRequest(title = "Create Request Validation Error", detail = Some("message"))
      assert(httpError)(equalTo(errorResponse))
    },
    test("return bad request error if credential id not defined error") {
      val w3cCredentialPayload = W3cCredentialPayload(
        `@context` = Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
        maybeId = Some("http://example.edu/credentials/3732"),
        `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
        issuer = DID("https://example.edu/issuers/565049"),
        issuanceDate = Instant.parse("2010-01-01T00:00:00Z"),
        maybeExpirationDate = Some(Instant.parse("2010-01-12T00:00:00Z")),
        maybeCredentialSchema = Some(
          CredentialSchema(
            id = "did:work:MDP8AsFhHzhwUvGNuYkX7T;id=06e126d1-fa44-4882-a243-1e326fbe21db;version=1.0",
            `type` = "JsonSchemaValidator2018"
          )
        ),
        credentialSubject = Json.obj(
          "userName" -> Json.fromString("Bob"),
          "age" -> Json.fromInt(42),
          "email" -> Json.fromString("email")
        ),
        maybeCredentialStatus = Some(
          CredentialStatus(
            id = "did:work:MDP8AsFhHzhwUvGNuYkX7T;id=06e126d1-fa44-4882-a243-1e326fbe21db;version=1.0",
            `type` = "CredentialStatusList2017"
          )
        ),
        maybeRefreshService = Some(
          RefreshService(
            id = "https://example.edu/refresh/3732",
            `type` = "ManualRefreshService2018"
          )
        ),
        maybeEvidence = Option.empty,
        maybeTermsOfUse = Option.empty
      )
      val cse = CredentialServiceError.CredentialIdNotDefined(w3cCredentialPayload)
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.badRequest(title = "Credential ID not defined one request", detail = Some(w3cCredentialPayload.toString))
      assert(httpError)(equalTo(errorResponse))
    },
    test("return internal server error if iris error") {
      val cse = CredentialServiceError.IrisError(new Throwable("message"))
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.internalServerError(title = "VDR Error", detail = Some(cse.cause.toString))
      assert(httpError)(equalTo(errorResponse))
    }
  )

}
