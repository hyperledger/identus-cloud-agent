package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import zio.*
import io.iohk.atala.agent.openapi.api.DIDRegistrarApiService
import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.agent.server.http.model.{HttpServiceError, OASDomainModelHelper, OASErrorModelHelper}
import io.iohk.atala.agent.walletapi.model.error.PublishManagedDIDError
import io.iohk.atala.castor.core.model.did.PrismDID

class DIDRegistrarApiServiceImpl(service: ManagedDIDService)(using runtime: Runtime[Any])
    extends DIDRegistrarApiService,
      AkkaZioSupport,
      OASDomainModelHelper,
      OASErrorModelHelper {

  private val mockDID = DID(
    id = "did:prism:1:mainnet:abcdef123456",
    controller = None,
    verificationMethod = None,
    authentication = Some(
      Seq(
        VerificationMethodOrRef(verificationMethod =
          Some(
            VerificationMethod(
              id = "did:prism:1:mainnet:abcdef123456#key-1",
              `type` = "JsonWebKey2020",
              controller = "did:prism:1:mainnet:abcdef123456",
              jsonWebKey2020 = JsonWebKey2020(
                publicKeyJwk = PublicKeyJwk(
                  crv = Some("P-256"),
                  x = Some("38M1FDts7Oea7urmseiugGW7tWc3mLpJh6rKe7xINZ8"),
                  y = Some("nDQW6XZ7b_u2Sy9slofYLlG03sOEoug3I0aAPQ0exs4"),
                  kty = Some("EC"),
                  kid = Some("_TKzHv2jFIyvdTGF1Dsgwngfdg3SH6TpDv0Ta1aOEkw")
                )
              )
            )
          )
        )
      )
    ),
    assertionMethod = None,
    keyAgreement = None,
    capabilityInvocation = None,
    service = None
  )

  // TODO: implement
  override def createManagedDid(createManagedDidRequest: CreateManagedDidRequest)(implicit
      toEntityMarshallerCreateManagedDIDResponse: ToEntityMarshaller[CreateManagedDIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ =>
      createManagedDid200(
        CreateManagedDIDResponse(
          did = mockDID,
          longFormDid = "did:prism:1:mainnet:abcdef:abcdef"
        )
      )
    }
  }

  override def publishManagedDid(didRef: String)(implicit
      toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      prismDID <- ZIO.fromEither(PrismDID.parse(didRef)).mapError(HttpServiceError.InvalidPayload.apply)
      outcome <- service.publishStoredDID(prismDID).mapError(HttpServiceError.DomainError[PublishManagedDIDError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.toOAS).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => publishManagedDid202(result)
    }
  }

}

object DIDRegistrarApiServiceImpl {
  val layer: URLayer[ManagedDIDService, DIDRegistrarApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[ManagedDIDService]
    } yield DIDRegistrarApiServiceImpl(svc)(using rt)
  }
}
