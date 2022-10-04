package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.agent.openapi.api.DIDApiService
import io.iohk.atala.agent.openapi.model.*
import zio.*

// TODO: replace with actual implementation
class DIDApiServiceImpl(service: DIDService)(using runtime: Runtime[Any]) extends DIDApiService with AkkaZioSupport {

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

  private val mockDIDResponse = DIDResponse(
    did = mockDID,
    `type` = DidTypeWithProof(`type` = "PUBLISHED", proof = None),
    deactivated = false
  )

  override def createDid(createDIDRequest: CreateDIDRequest)(implicit
      toEntityMarshallerDIDResponseWithAsyncOutcome: ToEntityMarshaller[DIDResponseWithAsyncOutcome],
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ => createDid200(mockDIDResponse) }
  }

  override def deactivateDID(didRef: String, deactivateDIDRequest: DeactivateDIDRequest)(implicit
      toEntityMarshallerDIDResponseWithAsyncOutcome: ToEntityMarshaller[DIDResponseWithAsyncOutcome],
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ => deactivateDID200(mockDIDResponse) }
  }

  override def getDid(didRef: String)(implicit
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ => getDid200(mockDIDResponse) }
  }

  override def recoverDid(didRef: String, recoverDIDRequest: RecoverDIDRequest)(implicit
      toEntityMarshallerDIDResponseWithAsyncOutcome: ToEntityMarshaller[DIDResponseWithAsyncOutcome],
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ => recoverDid200(mockDIDResponse) }
  }

  override def updateDid(didRef: String, updateDIDRequest: UpdateDIDRequest)(implicit
      toEntityMarshallerDIDResponseWithAsyncOutcome: ToEntityMarshaller[DIDResponseWithAsyncOutcome],
      toEntityMarshallerDIDResponse: ToEntityMarshaller[DIDResponse],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    onZioSuccess(ZIO.unit) { _ => updateDid200(mockDIDResponse) }
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
