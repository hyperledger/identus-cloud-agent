package io.iohk.atala.pollux

import io.iohk.atala.pollux.schema.VerificationPolicyServerEndpoints
import io.iohk.atala.pollux.schema.model.{VerificationPolicy, VerificationPolicyInput, VerificationPolicyPage}
import io.iohk.atala.pollux.service.{VerificationPolicyService, VerificationPolicyServiceInMemory}
import sttp.client3.DeserializationException
import sttp.client3.Response.ExampleGet.uri
import io.iohk.atala.api.http.{BadRequest, NotFound}
import zio.test.ZIOSpecDefault
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{DeserializationException, ResponseException, SttpBackend, UriContext, basicRequest}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.interceptor.RequestResult.Response
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.ZIO
import zio.test.*
import zio.test.Assertion.*
import sttp.model.{StatusCode, Uri}
import zio.json.{DecoderOps, EncoderOps, JsonDecoder}
import zio.stream.ZSink
import zio.stream.ZSink.*
import zio.stream.ZStream.unfold
import zio.test.Assertion.*
import zio.test.Gen.*
import zio.{Random, ZLayer, *}

import java.time.ZonedDateTime
import java.util.UUID

object VerificationPolicyEndpointsSpec extends ZIOSpecDefault:
  type VerificationPolicyResponse =
    sttp.client3.Response[Either[DeserializationException[String], VerificationPolicy]]
  type VerificationPolicyPageResponse =
    sttp.client3.Response[Either[DeserializationException[String], VerificationPolicyPage]]

  private val zonedDateTime = ZonedDateTime.now()
  private val verificationPolicyId = UUID.randomUUID().toString

  private val verificationPolicyInput = VerificationPolicyInput(
    id = Some(verificationPolicyId),
    name = "TestVerificationPolicies",
    attributes = List("first_name", "dob"),
    issuerDIDs = List("did:prism:abc", "did:prism:xyz"),
    credentialTypes = List("DrivingLicence", "ID"),
    createdAt = Some(ZonedDateTime.now()),
    updatedAt = Some(ZonedDateTime.now())
  )

  private val verificationPoliciesUri = uri"http://test.com/verification/policies"
  private val verificationPolicy = VerificationPolicy(verificationPolicyInput).withBaseUri(verificationPoliciesUri)

  def httpBackend(service: VerificationPolicyService) = {

    val endpoints = VerificationPolicyServerEndpoints(service)

    val backend =
      TapirStubInterpreter(SttpBackendStub(new RIOMonadError[Any]))
        .whenServerEndpoint(endpoints.createVerificationPolicyServerEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(endpoints.getVerificationPolicyByIdServerEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(endpoints.updateVerificationPolicyServerEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(endpoints.deleteVerificationPolicyByIdServerEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(endpoints.lookupVerificationPoliciesByQueryServerEndpoint)
        .thenRunLogic()
        .backend()
    backend
  }

  def spec = suite("verification policy endpoints spec")(
    crudOperationsSpec
  ).provideLayer(VerificationPolicyServiceInMemory.layer)

  private val crudOperationsSpec = suite("verification policy CRUD operations logic")(
    test("create the new policy") {
      for {
        service <- ZIO.service[VerificationPolicyService]
        backend = httpBackend(service)

        response = basicRequest
          .post(verificationPoliciesUri)
          .body(verificationPolicyInput.toJsonPretty)
          .response(asJsonAlways[VerificationPolicy])
          .send(backend)

        assertion <- assertZIO(response.map(_.body))(isRight(equalTo(verificationPolicy)))
      } yield assertion
    },
    test("create and get the policy by id") {
      for {
        service <- ZIO.service[VerificationPolicyService]
        backend = httpBackend(service)

        _ <- basicRequest
          .post(verificationPoliciesUri)
          .body(verificationPolicyInput.toJsonPretty)
          .send(backend)

        response = basicRequest
          .get(verificationPoliciesUri.addPath(verificationPolicyId))
          .response(asJsonAlways[VerificationPolicy])
          .send(backend)

        policyIsReturnedById <- assertZIO(response.map(_.body))(isRight(equalTo(verificationPolicy)))

        updateInput = verificationPolicyInput.copy(name = "new name")
        updateResponse: VerificationPolicyResponse <- basicRequest
          .put(verificationPoliciesUri.addPath(verificationPolicyId))
          .body(updateInput.toJsonPretty)
          .response(asJsonAlways[VerificationPolicy])
          .send(backend)
        updatedVerificationPolicy <- ZIO.fromEither(updateResponse.body)

        verificationPolicyNameIsUpdatedById <- assert(updatedVerificationPolicy.name)(equalTo(updateInput.name))
      } yield policyIsReturnedById && verificationPolicyNameIsUpdatedById
    },
    test("create and get the policy by the wrong id") {
      for {
        service <- ZIO.service[VerificationPolicyService]
        backend = httpBackend(service)

        _ <- basicRequest
          .post(verificationPoliciesUri)
          .body(verificationPolicyInput.toJsonPretty)
          .send(backend)

        uuid = UUID.randomUUID().toString

        response = basicRequest
          .get(verificationPoliciesUri.addPath(uuid))
          .response(asJsonAlways[NotFound])
          .send(backend)

        assertion <- assertZIO(response.map(_.code))(equalTo(StatusCode.NotFound))
      } yield assertion
    }
  )
