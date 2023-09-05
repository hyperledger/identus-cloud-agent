package io.iohk.atala.pollux.credentialdefinition

import io.iohk.atala.agent.walletapi.memory.DIDSecretStorageInMemory
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.agent.walletapi.model.PublicationState
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.agent.walletapi.service.MockManagedDIDService
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.castor.core.model.did.PrismDIDOperation
import io.iohk.atala.container.util.PostgresLayer.*
import io.iohk.atala.pollux.core.repository.CredentialDefinitionRepository
import io.iohk.atala.pollux.core.service.CredentialDefinitionServiceImpl
import io.iohk.atala.pollux.core.service.ResourceURIDereferencerImpl
import io.iohk.atala.pollux.credentialdefinition.controller.CredentialDefinitionController
import io.iohk.atala.pollux.credentialdefinition.controller.CredentialDefinitionControllerImpl
import io.iohk.atala.pollux.credentialdefinition.http.CredentialDefinitionInput
import io.iohk.atala.pollux.credentialdefinition.http.CredentialDefinitionResponse
import io.iohk.atala.pollux.credentialdefinition.http.CredentialDefinitionResponsePage
import io.iohk.atala.pollux.sql.repository.JdbcCredentialDefinitionRepository
import sttp.client3.DeserializationException
import sttp.client3.Response
import sttp.client3.UriContext
import sttp.client3.basicRequest
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.EncoderOps
import zio.mock.Expectation
import zio.test.Assertion
import zio.test.Gen
import zio.test.ZIOSpecDefault

import java.time.OffsetDateTime

trait CredentialDefinitionTestTools {
  self: ZIOSpecDefault =>

  type CredentialDefinitionBadRequestResponse =
    Response[Either[DeserializationException[String], ErrorResponse]]
  type CredentialDefinitionResponseType =
    Response[Either[DeserializationException[String], CredentialDefinitionResponse]]
  type CredentialDefinitionResponsePageType =
    Response[
      Either[DeserializationException[String], CredentialDefinitionResponsePage]
    ]

  private val pgLayer = postgresLayer(verbose = false)
  private val transactorLayer = pgLayer >>> hikariConfigLayer >>> transactor
  private val jdbcCredentialDefinitionRepository = transactorLayer >>> JdbcCredentialDefinitionRepository.layer
  private val controllerLayer =
    DIDSecretStorageInMemory.layer >+>
      jdbcCredentialDefinitionRepository >+>
      ResourceURIDereferencerImpl.layer >+>
      CredentialDefinitionServiceImpl.layer >+>
      CredentialDefinitionControllerImpl.layer

  val mockManagedDIDServiceLayer: Expectation[ManagedDIDService] = MockManagedDIDService
    .GetManagedDIDState(
      assertion = Assertion.anything,
      result = Expectation.value(
        Some(
          ManagedDIDState(
            PrismDIDOperation.Create(Nil, Nil, Nil),
            0,
            PublicationState.Published(scala.collection.immutable.ArraySeq.empty)
          )
        )
      )
    )

  val testEnvironmentLayer = zio.test.testEnvironment ++
    pgLayer ++
    transactorLayer ++
    controllerLayer

  val credentialDefinitionUriBase = uri"http://test.com/credential-definition-registry/definitions"

  def bootstrapOptions[F[_]](monadError: MonadError[F]) = {
    new CustomiseInterceptors[F, Any](_ => ())
      .defaultHandlers(ErrorResponse.failureResponseHandler)
  }

  def httpBackend(controller: CredentialDefinitionController) = {
    val credentialDefinitionRegistryEndpoints = CredentialDefinitionRegistryServerEndpoints(controller)

    val backend =
      TapirStubInterpreter(
        bootstrapOptions(new RIOMonadError[Any]),
        SttpBackendStub(new RIOMonadError[Any])
      )
        .whenServerEndpoint(credentialDefinitionRegistryEndpoints.createCredentialDefinitionServerEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(credentialDefinitionRegistryEndpoints.getCredentialDefinitionByIdServerEndpoint)
        .thenRunLogic()
        .whenServerEndpoint(
          credentialDefinitionRegistryEndpoints.lookupCredentialDefinitionsByQueryServerEndpoint
        )
        .thenRunLogic()
        .backend()
    backend
  }

  def deleteAllCredentialDefinitions: RIO[CredentialDefinitionRepository[Task], Long] = {
    for {
      repository <- ZIO.service[CredentialDefinitionRepository[Task]]
      count <- repository.deleteAll()
    } yield count
  }
}

trait CredentialDefinitionGen {
  self: ZIOSpecDefault with CredentialDefinitionTestTools =>
  object Generator {
    val credentialDefinitionName = Gen.alphaNumericStringBounded(4, 12)
    val majorVersion = Gen.int(1, 9)
    val minorVersion = Gen.int(0, 9)
    val patchVersion = Gen.int(0, 9)
    val credentialDefinitionVersion =
      majorVersion <*> minorVersion <*> patchVersion map (v => s"${v._1}.${v._2}.${v._3}")
    val credentialDefinitionDescription = Gen.alphaNumericStringBounded(5, 30)
    val credentialDefinitionAttribute = Gen.alphaNumericStringBounded(3, 9)
    val credentialDefinitionAttributes = Gen.setOfBounded(1, 4)(credentialDefinitionAttribute).map(_.toList)
    val credentialDefinitionAuthored = Gen.offsetDateTime(
      min = OffsetDateTime.now().minusMonths(6),
      max = OffsetDateTime.now()
    )
    val credentialDefinitionTag: Gen[Any, String] = Gen.alphaNumericStringBounded(3, 5)

    val credentialDefinitionAuthor =
      Gen.int(1000000, 9999999).map(i => s"did:prism:4fb06243213500578f59588de3e1dd9b266ec1b61e43b0ff86ad0712f$i")

    val credentialDefinitionSchemaId = Gen.alphaNumericStringBounded(4, 12)
    val credentialDefinitionSignatureType = Gen.alphaNumericStringBounded(4, 12)
    val credentialDefinitionSupportRevocation = Gen.boolean

    val definitionString =
      """
        |{
        |   "credentialSubject": {
        |     "emailAddress": "alice@wonderland.com",
        |     "givenName": "Alice",
        |     "familyName": "Wonderland",
        |     "dateOfIssuance": "2000-01-01T10:00:00Z",
        |     "drivingLicenseID": "12345",
        |     "drivingClass": 5
        |   }
        |}
        |""".stripMargin

    val credentialDefinitionInput = for {
      name <- credentialDefinitionName
      version <- credentialDefinitionVersion
      description <- credentialDefinitionDescription
      author <- credentialDefinitionAuthor
      tag <- credentialDefinitionTag
      signatureType <- credentialDefinitionSignatureType
      supportRevocation <- credentialDefinitionSupportRevocation
    } yield CredentialDefinitionInput(
      name = name,
      version = version,
      description = Some(description),
      tag = tag,
      author = author,
      schemaId = "resource:///anoncred-schema-example.json",
      signatureType = signatureType,
      supportRevocation = supportRevocation
    )
  }

  def generateCredentialDefinitionsN(
      count: Int
  ): ZIO[CredentialDefinitionController, Throwable, List[CredentialDefinitionInput]] =
    for {
      controller <- ZIO.service[CredentialDefinitionController]
      backend = httpBackend(controller)
      inputs <- Generator.credentialDefinitionInput.runCollectN(count)
      _ <- inputs
        .map(in =>
          basicRequest
            .post(credentialDefinitionUriBase)
            .body(in.toJsonPretty)
            .response(asJsonAlways[CredentialDefinitionResponse])
            .send(backend)
        )
        .reduce((l, r) => l.flatMap(_ => r))
    } yield inputs
}
