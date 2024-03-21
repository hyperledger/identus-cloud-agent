package io.iohk.atala.issue.controller

import com.typesafe.config.ConfigFactory
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.http.CustomServerInterceptors
import io.iohk.atala.agent.walletapi.memory.GenericSecretStorageInMemory
import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.service.MockManagedDIDService
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.castor.core.service.MockDIDService
import io.iohk.atala.connect.core.repository.ConnectionRepositoryInMemory
import io.iohk.atala.connect.core.service.ConnectionServiceImpl
import io.iohk.atala.iam.authentication.AuthenticatorWithAuthZ
import io.iohk.atala.iam.authentication.DefaultEntityAuthenticator
import io.iohk.atala.issue.controller.http.{
  CreateIssueCredentialRecordRequest,
  IssueCredentialRecord,
  IssueCredentialRecordPage
}
import io.iohk.atala.pollux.anoncreds.AnoncredLinkSecretWithId
import io.iohk.atala.pollux.core.model.CredentialFormat
import io.iohk.atala.pollux.core.repository.{
  CredentialDefinitionRepositoryInMemory,
  CredentialRepositoryInMemory,
  CredentialStatusListRepositoryInMemory
}
import io.iohk.atala.pollux.core.service.*
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import io.iohk.atala.sharedtest.containers.PostgresTestContainerSupport
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{DeserializationException, Response, UriContext}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.config.typesafe.TypesafeConfigSource
import zio.config.{ReadError, read}
import zio.json.ast.Json
import zio.json.ast.Json.*
import zio.test.*
import java.util.UUID

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

  val configLayer: Layer[ReadError[String], AppConfig] = ZLayer.fromZIO {
    read(
      AppConfig.descriptor.from(
        TypesafeConfigSource.fromTypesafeConfig(
          ZIO.attempt(ConfigFactory.load())
        )
      )
    )
  }

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

  private val controllerLayer = contextAwareTransactorLayer >+>
    configLayer >+>
    didResolverLayer >+>
    ResourceURIDereferencerImpl.layer >+>
    CredentialRepositoryInMemory.layer >+>
    CredentialStatusListRepositoryInMemory.layer >+>
    ZLayer.succeed(AnoncredLinkSecretWithId("Unused Linked Secret ID")) >+>
    MockDIDService.empty >+>
    MockManagedDIDService.empty >+>
    CredentialServiceImpl.layer >+>
    ConnectionRepositoryInMemory.layer >+>
    ConnectionServiceImpl.layer >+>
    IssueControllerImpl.layer

  protected val defaultWalletLayer = ZLayer.succeed(WalletAccessContext(WalletId.default))

  protected val credentialDefinitionServiceLayer =
    CredentialDefinitionRepositoryInMemory.layer ++ ResourceURIDereferencerImpl.layer >>>
      CredentialDefinitionServiceImpl.layer

  val testEnvironmentLayer = zio.test.testEnvironment ++
    pgContainerLayer ++
    contextAwareTransactorLayer ++
    GenericSecretStorageInMemory.layer >+> LinkSecretServiceImpl.layer ++
    GenericSecretStorageInMemory.layer >+> credentialDefinitionServiceLayer >+>
    controllerLayer ++
    DefaultEntityAuthenticator.layer

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

trait IssueGen {
  self: ZIOSpecDefault with IssueControllerTestTools =>
  object Generator {
    val gValidityPeriod: Gen[Any, Double] = Gen.double
    val gAutomaticIssuance: Gen[Any, Boolean] = Gen.boolean
    val gIssuingDID: Gen[Any, String] = Gen.alphaNumericStringBounded(5, 20) // TODO Make a DID generator
    val gConnectionId: Gen[Any, UUID] = Gen.uuid

    val claims = Json.Obj(
      "key1" -> Json.Str("value1"),
      "key2" -> Json.Str("value2")
    )

    val schemaInput = for {
      validityPeriod <- gValidityPeriod
      automaticIssuance <- gAutomaticIssuance
      issuingDID <- gIssuingDID
      connectionId <- gConnectionId
    } yield CreateIssueCredentialRecordRequest(
      validityPeriod = Some(validityPeriod),
      schemaId = None,
      claims = claims,
      automaticIssuance = Some(automaticIssuance),
      issuingDID = Some(issuingDID),
      connectionId = connectionId,
      credentialDefinitionId = None,
      credentialFormat = Some(CredentialFormat.JWT.toString)
    )
  }

}
