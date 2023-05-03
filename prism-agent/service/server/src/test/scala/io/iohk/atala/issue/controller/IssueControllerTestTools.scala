package io.iohk.atala.issue.controller

import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.connect.core.service.ConnectionServiceImpl
import io.iohk.atala.connect.sql.repository.JdbcConnectionRepository
import io.iohk.atala.pollux.core.repository.CredentialSchemaRepository
import io.iohk.atala.pollux.core.service.CredentialSchemaServiceImpl
import io.iohk.atala.pollux.credentialschema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.credentialschema.controller.{CredentialSchemaController, CredentialSchemaControllerImpl}
import io.iohk.atala.pollux.credentialschema.http.{CredentialSchemaInput, CredentialSchemaResponse, CredentialSchemaResponsePage}
import io.iohk.atala.pollux.sql.repository.JdbcCredentialSchemaRepository
import io.iohk.atala.container.util.MigrationAspects.*
import io.iohk.atala.container.util.PostgresLayer.*
import io.iohk.atala.issue.controller.http.{CreateIssueCredentialRecordRequest, IssueCredentialRecord, IssueCredentialRecordPage}
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{DeserializationException, Response, ResponseException, SttpBackend, UriContext, basicRequest}
import sttp.model.{StatusCode, Uri}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.json.ast.Json.*
import zio.json.{DecoderOps, EncoderOps, JsonDecoder}
import zio.stream.ZSink
import zio.stream.ZSink.*
import zio.stream.ZStream.unfold
import zio.test.TestAspect.*
import zio.test.{Gen, Spec, ZIOSpecDefault}
import zio.{RIO, Task, URLayer, ZIO, ZLayer}

import java.time.OffsetDateTime

trait IssueControllerTestTools {
  self: ZIOSpecDefault =>

  type IssueCredentialBadRequestResponse =
    Response[Either[DeserializationException[String], ErrorResponse]]
  type IssueCredentialResponse =
    Response[Either[DeserializationException[String], IssueCredentialRecord]]
  type IssueCredentialPageResponse =
    Response[
      Either[DeserializationException[String], IssueCredentialRecordPage]
    ]

  private val pgLayer = postgresLayer(verbose = false)
  private val transactorLayer = pgLayer >>> hikariConfigLayer >>> transactor
  private val controllerLayer = transactorLayer >>>
    JdbcCredentialSchemaRepository.layer >+>
    CredentialSchemaServiceImpl.layer >+>
    JdbcConnectionRepository.layer >+>
    ConnectionServiceImpl.layer >+>
    IssueControllerImpl.layer

  val testEnvironmentLayer = zio.test.testEnvironment ++
    pgLayer ++
    transactorLayer ++
    controllerLayer

  val issueUriBase = uri"http://test.com/issue-credentials/"

  def bootstrapOptions[F[_]](monadError: MonadError[F]): CustomiseInterceptors[F, Any] = {
    import sttp.tapir.server.interceptor.RequestResult.Response
    new CustomiseInterceptors[F, Any](_ => ())
      .defaultHandlers(ErrorResponse.failureResponseHandler)
  }

  def httpBackend(controller: IssueController): SttpBackend[_, _] = {
    val issueEndpoints = IssueServerEndpoints(controller)

    val backend =
      TapirStubInterpreter(
        bootstrapOptions(new RIOMonadError[Any]),
        SttpBackendStub(new RIOMonadError[Any])
      )
        .whenServerEndpoint(issueEndpoints.createCredentialOfferEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(issueEndpoints.getCredentialRecordsEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(issueEndpoints.getCredentialRecordEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(issueEndpoints.acceptCredentialOfferEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(issueEndpoints.issueCredentialEndpoint)
        .thenRunLogic()
        .backend()
    backend
  }

}

trait IssueGen {
  self: ZIOSpecDefault with IssueControllerTestTools =>
  object Generator {
    val gValidityPeriod: Gen[Any, Double] = Gen.double
    val gClaims: Gen[Any, Map[String, String]] = Gen.mapOf(Gen.alphaNumericStringBounded(5, 20), Gen.alphaNumericStringBounded(5, 20))
    val gAutomaticIssuance: Gen[Any, Boolean] = Gen.boolean
    val gIssuingDID: Gen[Any, String] = Gen.alphaNumericStringBounded(5,20) //TODO Make a DID generator
    val gConnectionId: Gen[Any, String] = Gen.alphaNumericStringBounded(5, 20)

    val schemaInput = for {
      validityPeriod <- gValidityPeriod
      claims <- gClaims
      automaticIssuance <- gAutomaticIssuance
      issuingDID <- gIssuingDID
      connectionId <- gConnectionId
    } yield CreateIssueCredentialRecordRequest(
      validityPeriod = Some(validityPeriod),
      claims = claims,
      automaticIssuance = Some(automaticIssuance),
      issuingDID = issuingDID,
      connectionId = connectionId
    )
  }

//  def generateCreateIssueCredentialRecordRequetN(
//      count: Int
//  ): ZIO[IssueController, Throwable, List[CreateIssueCredentialRecordRequest]] =
//    for {
//      controller <- ZIO.service[IssueController]
//      backend = httpBackend(controller)
//      inputs <- Generator.schemaInput.runCollectN(count)
//      _ <- inputs
//        .map(in =>
//          basicRequest
//            .post(uri"${issueUriBase}/credential-offers")
//            .body(in.toJsonPretty)
//            .response(asJsonAlways[IssueCredentialRecord])
//            .send(backend)
//        )
//        .reduce((l, r) => l.flatMap(_ => r))
//    } yield inputs
}
