package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.did.w3c.W3CModelHelper.*
import io.iohk.atala.castor.core.model.did.w3c.makeW3CResolver
import io.iohk.atala.agent.openapi.api.DIDApiService
import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.agent.server.http.model.{HttpServiceError, OASDomainModelHelper, OASErrorModelHelper}
import io.iohk.atala.castor.core.model.error.DIDOperationError
import zio.*

// TODO: replace with actual implementation
class DIDApiServiceImpl(service: DIDService)(using runtime: Runtime[Any])
    extends DIDApiService,
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
              publicKeyJwk = PublicKeyJwk(
                crv = Some("P-256"),
                x = Some("38M1FDts7Oea7urmseiugGW7tWc3mLpJh6rKe7xINZ8"),
                y = Some("nDQW6XZ7b_u2Sy9slofYLlG03sOEoug3I0aAPQ0exs4"),
                kty = "EC",
                kid = Some("_TKzHv2jFIyvdTGF1Dsgwngfdg3SH6TpDv0Ta1aOEkw")
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

  private val mockDIDResponse = DIDResponse(
    did = mockDID,
    metadata = DIDDocumentMetadata(deactivated = false)
  )

  private val mockDIDOperationResponse = DIDOperationResponse(
    scheduledOperation = DidOperationSubmission(
      id = "0123456789abcdef",
      didRef = "did:example:123"
    )
  )

  override def createDid(createDIDRequest: CreateDIDRequest)(implicit
      toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ => createDid202(mockDIDOperationResponse) }
  }

  override def deactivateDID(didRef: String, deactivateDIDRequest: DeactivateDIDRequest)(implicit
      toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ => deactivateDID202(mockDIDOperationResponse) }
  }

  override def getDid(didRef: String)(implicit
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = makeW3CResolver(service)(didRef).mapError(HttpServiceError.DomainError.apply)
    onZioSuccess(result.mapBoth(_.toOAS, _.toOAS).either) {
      case Left(error)     => complete(error.status -> error)
      case Right(response) => getDid200(response)
    }
  }

  override def recoverDid(didRef: String, recoverDIDRequest: RecoverDIDRequest)(implicit
      toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ => recoverDid202(mockDIDOperationResponse) }
  }

  override def updateDid(didRef: String, updateDIDRequest: UpdateDIDRequest)(implicit
      toEntityMarshallerDIDOperationResponse: ToEntityMarshaller[DIDOperationResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ => updateDid202(mockDIDOperationResponse) }
  }

}

object DIDApiServiceImpl {
  val layer: URLayer[DIDService, DIDApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[DIDService]
    } yield DIDApiServiceImpl(svc)(using rt)
  }
}
