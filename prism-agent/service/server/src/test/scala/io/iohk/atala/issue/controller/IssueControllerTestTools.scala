package io.iohk.atala.issue.controller

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.typesafe.config.ConfigFactory
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.http.CustomServerInterceptors
import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.iam.authentication.{AuthenticatorWithAuthZ, DefaultEntityAuthenticator}
import io.iohk.atala.issue.controller.http.IssueCredentialRecordPage
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.service.*
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.sharedtest.containers.PostgresTestContainerSupport
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{DeserializationException, Response, UriContext}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.test.*

trait IssueControllerTestTools extends PostgresTestContainerSupport {
  self: ZIOSpecDefault =>

  type IssueCredentialBadRequestResponse =
    Response[Either[DeserializationException[String], ErrorResponse]]
  type IssueCredentialResponse =
    Response[Either[DeserializationException[String], IssueCredentialRecord]]
  type IssueCredentialPageResponse =
    Response[
      Either[DeserializationException[String], IssueCredentialRecordPage]
    ]
  val didResolverLayer = ZLayer.fromZIO(ZIO.succeed(makeResolver(Map.empty)))

  val configLayer = ZLayer.fromZIO(
    TypesafeConfigProvider
      .fromTypesafeConfig(ConfigFactory.load())
      .load(AppConfig.config)
  )

  private[this] def makeResolver(lookup: Map[String, DIDDocument]): DidResolver = (didUrl: String) => {
    lookup
      .get(didUrl)
      .fold(
        ZIO.succeed(DIDResolutionFailed(NotFound(s"DIDDocument not found for $didUrl")))
      )((didDocument: DIDDocument) => {
        ZIO.succeed(
          DIDResolutionSucceeded(
            didDocument,
            DIDDocumentMetadata()
          )
        )
      })
  }

  lazy val testEnvironmentLayer =
    ZLayer.makeSome[
      ManagedDIDService & DIDService & CredentialService & ConnectionService,
      IssueController & AppConfig & PostgreSQLContainer & AuthenticatorWithAuthZ[BaseEntity]
    ](IssueControllerImpl.layer, configLayer, pgContainerLayer, DefaultEntityAuthenticator.layer)

  val issueUriBase = uri"http://test.com/issue-credentials/"

  def bootstrapOptions[F[_]](monadError: MonadError[F]): CustomiseInterceptors[F, Any] = {
    new CustomiseInterceptors[F, Any](_ => ())
      .exceptionHandler(CustomServerInterceptors.exceptionHandler)
      .rejectHandler(CustomServerInterceptors.rejectHandler)
      .decodeFailureHandler(CustomServerInterceptors.decodeFailureHandler)
  }

  def httpBackend(controller: IssueController, authenticator: AuthenticatorWithAuthZ[BaseEntity]) = {
    val issueEndpoints = IssueServerEndpoints(controller, authenticator, authenticator)

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
