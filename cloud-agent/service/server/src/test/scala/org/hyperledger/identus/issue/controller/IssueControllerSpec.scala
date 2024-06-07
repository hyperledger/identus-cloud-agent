package org.hyperledger.identus.issue.controller

import io.circe.*
import org.hyperledger.identus.api.http.ErrorResponse
import org.hyperledger.identus.pollux.core.model.error.CredentialServiceError
import org.hyperledger.identus.pollux.core.model.error.CredentialServiceErrorNew.UnsupportedDidFormat
import org.hyperledger.identus.pollux.core.model.DidCommID
import org.hyperledger.identus.pollux.vc.jwt.{W3cCredentialPayload, *}
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.time.Instant

object IssueControllerSpec extends ZIOSpecDefault {

  override def spec = suite("IssueControllerSpec")(httpErrorSpec)

  private val httpErrorSpec = suite("testHttpErrors")(
    test("return not found error if record id not found") {
      val cse = CredentialServiceError.RecordIdNotFound(DidCommID("12345"))
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.notFound(detail = Some(s"Record Id not found: 12345"))
      assert(httpError)(equalTo(errorResponse.copy(instance = httpError.instance)))
    },
    test("return internal server error if operation not executed") {
      val cse = CredentialServiceError.OperationNotExecuted(DidCommID("12345"), "info")
      val httpError = IssueController.toHttpError(cse)
      val errorResponse =
        ErrorResponse.internalServerError(title = "Operation Not Executed", detail = Some(s"12345-info"))
      assert(httpError)(equalTo(errorResponse.copy(instance = httpError.instance)))
    },
    test("return not found error if thread Id not found") {
      val cse = CredentialServiceError.ThreadIdNotFound(DidCommID("12345"))
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.notFound(detail = Some(s"Thread Id not found: 12345"))
      assert(httpError)(equalTo(errorResponse.copy(instance = httpError.instance)))
    },
    test("return internal server error if unexpected error") {
      val cse = CredentialServiceError.UnexpectedError("message")
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.internalServerError(detail = Some("message"))
      assert(httpError)(equalTo(errorResponse.copy(instance = httpError.instance)))
    },
    test("return bad request error if invalid flow state error") {
      val cse = CredentialServiceError.InvalidFlowStateError("message")
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.badRequest(title = "InvalidFlowState", detail = Some("message"))
      assert(httpError)(equalTo(errorResponse.copy(instance = httpError.instance)))
    },
    test("return bad request error if create credential payload from record error") {
      val cse = CredentialServiceError.CreateCredentialPayloadFromRecordError(new Throwable("message"))
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse
        .badRequest(title = "Create Credential Payload From Record Error", detail = Some(cse.cause.getMessage))
      assert(httpError)(equalTo(errorResponse.copy(instance = httpError.instance)))
    },
    test("return bad request error if create request validation error") {
      val cse = CredentialServiceError.CredentialRequestValidationError("message")
      val httpError = IssueController.toHttpError(cse)
      val errorResponse = ErrorResponse.badRequest(title = "Create Request Validation Error", detail = Some("message"))
      assert(httpError)(equalTo(errorResponse.copy(instance = httpError.instance)))
    },
  )

}
