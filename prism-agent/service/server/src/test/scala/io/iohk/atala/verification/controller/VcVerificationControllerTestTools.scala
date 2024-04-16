package io.iohk.atala.verification.controller

import com.typesafe.config.ConfigFactory
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.http.CustomServerInterceptors
import io.iohk.atala.agent.walletapi.crypto.{Prism14ECPrivateKey, Prism14ECPublicKey}
import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.castor.core.service.MockDIDService
import io.iohk.atala.iam.authentication.{AuthenticatorWithAuthZ, DefaultEntityAuthenticator}
import io.iohk.atala.pollux.core.service.*
import io.iohk.atala.pollux.core.service.verification.{VcVerificationService, VcVerificationServiceImpl}
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.shared.models.WalletId.*
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import io.iohk.atala.sharedtest.containers.PostgresTestContainerSupport
import sttp.client3.UriContext
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.config.typesafe.TypesafeConfigSource
import zio.config.{ReadError, read}
import zio.test.*

trait VcVerificationControllerTestTools extends PostgresTestContainerSupport {
  self: ZIOSpecDefault =>

  protected val (issuerOp, issuerKp, issuerDidMetadata, issuerDidData) =
    MockDIDService.createDID(VerificationRelationship.AssertionMethod)

  protected val issuer =
    Issuer(
      did = io.iohk.atala.pollux.vc.jwt.DID(issuerDidData.id.did.toString),
      signer = ES256KSigner(Prism14ECPrivateKey(issuerKp.getPrivateKey).toJavaPrivateKey),
      publicKey = Prism14ECPublicKey(issuerKp.getPublicKey).toJavaPublicKey
    )

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

  protected val defaultWalletLayer = ZLayer.succeed(WalletAccessContext(WalletId.default))

  lazy val testEnvironmentLayer =
    zio.test.testEnvironment ++ ZLayer.makeSome[
      ManagedDIDService,
      VcVerificationController & VcVerificationService & AuthenticatorWithAuthZ[BaseEntity]
    ](
      didResolverLayer,
      ResourceURIDereferencerImpl.layer,
      VcVerificationControllerImpl.layer,
      VcVerificationServiceImpl.layer,
      DefaultEntityAuthenticator.layer
    )

  val vcVerificationUriBase = uri"http://test.com/verification/credential"

  def bootstrapOptions[F[_]](monadError: MonadError[F]): CustomiseInterceptors[F, Any] = {
    new CustomiseInterceptors[F, Any](_ => ())
      .exceptionHandler(CustomServerInterceptors.exceptionHandler)
      .rejectHandler(CustomServerInterceptors.rejectHandler)
      .decodeFailureHandler(CustomServerInterceptors.decodeFailureHandler)
  }

  def httpBackend(controller: VcVerificationController, authenticator: AuthenticatorWithAuthZ[BaseEntity]) = {
    val vcVerificationEndpoints = VcVerificationServerEndpoints(controller, authenticator, authenticator)
    val backend =
      TapirStubInterpreter(
        bootstrapOptions(new RIOMonadError[Any]),
        SttpBackendStub(new RIOMonadError[Any])
      )
        .whenServerEndpoint(vcVerificationEndpoints.verifyEndpoint)
        .thenRunLogic()
        .backend()
    backend
  }

}
