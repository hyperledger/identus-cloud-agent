package io.iohk.atala.pollux.core.service.verification

import io.iohk.atala.agent.walletapi.crypto.{Prism14ECPrivateKey, Prism14ECPublicKey}
import io.iohk.atala.agent.walletapi.service.{ManagedDIDService, MockManagedDIDService}
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.castor.core.service.{DIDService, MockDIDService}
import io.iohk.atala.pollux.core.service.{ResourceURIDereferencerImpl, URIDereferencer}
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.shared.models.WalletId.*
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import zio.*
import zio.mock.Expectation

trait VcVerificationServiceSpecHelper {
  protected val defaultWalletLayer: ULayer[WalletAccessContext] = ZLayer.succeed(WalletAccessContext(WalletId.default))

  protected val (issuerOp, issuerKp, issuerDidMetadata, issuerDidData) =
    MockDIDService.createDID(VerificationRelationship.AssertionMethod)

  protected val issuer =
    Issuer(
      did = io.iohk.atala.pollux.vc.jwt.DID(issuerDidData.id.did.toString),
      signer = ES256KSigner(Prism14ECPrivateKey(issuerKp.getPrivateKey).toJavaPrivateKey),
      publicKey = Prism14ECPublicKey(issuerKp.getPublicKey).toJavaPublicKey
    )

  protected val issuerDidServiceExpectations: Expectation[DIDService] =
    MockDIDService.resolveDIDExpectation(issuerDidMetadata, issuerDidData)

  protected val issuerManagedDIDServiceExpectations: Expectation[ManagedDIDService] =
    MockManagedDIDService.getManagedDIDStateExpectation(issuerOp)
      ++ MockManagedDIDService.javaKeyPairWithDIDExpectation(issuerKp)

  protected val issuerDidResolverLayer: ZLayer[Any, Nothing, PrismDidResolver] = (issuerDidServiceExpectations ++
    issuerManagedDIDServiceExpectations).toLayer >>> ZLayer.fromFunction(PrismDidResolver(_))

  protected val emptyDidResolverLayer: ZLayer[Any, Nothing, PrismDidResolver] = MockDIDService.empty ++
    MockManagedDIDService.empty >>> ZLayer.fromFunction(PrismDidResolver(_))

  protected val vcVerificationServiceLayer: ZLayer[Any, Nothing, VcVerificationService with WalletAccessContext] =
    emptyDidResolverLayer ++ ResourceURIDereferencerImpl.layer >>>
      VcVerificationServiceImpl.layer ++ defaultWalletLayer

  protected val someVcVerificationServiceLayer
      : URLayer[DIDService & ManagedDIDService & URIDereferencer, VcVerificationService] =
    ZLayer.makeSome[DIDService & ManagedDIDService & URIDereferencer, VcVerificationService](
      ZLayer.fromFunction(PrismDidResolver(_)),
      VcVerificationServiceImpl.layer
    )

}
