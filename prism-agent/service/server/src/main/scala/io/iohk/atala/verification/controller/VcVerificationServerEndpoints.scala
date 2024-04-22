package io.iohk.atala.verification.controller

import io.iohk.atala.LogUtils.*
import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.iam.authentication.{Authenticator, Authorizer, DefaultAuthenticator, SecurityLogic}
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.verification.controller
import io.iohk.atala.verification.controller.VcVerificationEndpoints.verify
import sttp.tapir.ztapir.*
import zio.*

class VcVerificationServerEndpoints(
    vcVerificationController: VcVerificationController,
    authenticator: Authenticator[BaseEntity],
    authorizer: Authorizer[BaseEntity]
) {

  val verifyEndpoint: ZServerEndpoint[Any, Any] =
    verify
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac =>
        { case (ctx: RequestContext, request: List[controller.http.VcVerificationRequest]) =>
          vcVerificationController
            .verify(request)(ctx)
            .provideSomeLayer(ZLayer.succeed(wac))
            .logTrace(ctx)
        }
      }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    verifyEndpoint
  )

}

object VcVerificationServerEndpoints {
  def all: URIO[VcVerificationController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
      vcVerificationController <- ZIO.service[VcVerificationController]
      vcVerificationProofEndpoints =
        new VcVerificationServerEndpoints(
          vcVerificationController,
          authenticator,
          authenticator
        )
    } yield vcVerificationProofEndpoints.all
  }
}
