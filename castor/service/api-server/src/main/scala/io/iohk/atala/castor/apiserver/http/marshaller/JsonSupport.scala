package io.iohk.atala.castor.apiserver.http.marshaller

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.iohk.atala.castor.openapi.model.*
import spray.json.{DefaultJsonProtocol, RootJsonFormat, jsonReader}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  given RootJsonFormat[AuthenticationChallengeSubmissionRequest] = jsonFormat3(
    AuthenticationChallengeSubmissionRequest.apply
  )
  given RootJsonFormat[AuthenticationChallengeSubmissionResponse] = jsonFormat2(
    AuthenticationChallengeSubmissionResponse.apply
  )
  given RootJsonFormat[CreateAuthenticationChallengeRequest] = jsonFormat3(CreateAuthenticationChallengeRequest.apply)
  given RootJsonFormat[CreateAuthenticationChallengeResponse] = jsonFormat2(CreateAuthenticationChallengeResponse.apply)
  given RootJsonFormat[CreateDIDRequest] = jsonFormat7(CreateDIDRequest.apply)
  given RootJsonFormat[CreateDIDRequestDocument] = jsonFormat2(CreateDIDRequestDocument.apply)
  given RootJsonFormat[DeactivateDIDRequest] = jsonFormat5(DeactivateDIDRequest.apply)
  given RootJsonFormat[Delta] = jsonFormat2(Delta.apply)
  given RootJsonFormat[DeltaUpdate] = jsonFormat2(DeltaUpdate.apply)
  given RootJsonFormat[DID] = jsonFormat8(DID.apply)
  given RootJsonFormat[DidOperation] = jsonFormat4(DidOperation.apply)
  given RootJsonFormat[DidOperationStatus] = jsonFormat0(DidOperationStatus.apply)
  given RootJsonFormat[DidOperationType] = jsonFormat0(DidOperationType.apply)
  given RootJsonFormat[DIDResponse] = jsonFormat3(DIDResponse.apply)
  given RootJsonFormat[DIDResponseWithAsyncOutcome] = jsonFormat4(DIDResponseWithAsyncOutcome.apply)
  given RootJsonFormat[DidType] = jsonFormat0(DidType.apply)
  given RootJsonFormat[DidTypeWithProof] = jsonFormat2(DidTypeWithProof.apply)
  given RootJsonFormat[DidTypeWithSignedProof] = jsonFormat2(DidTypeWithSignedProof.apply)
  given RootJsonFormat[ErrorResponse] = jsonFormat5(ErrorResponse.apply)
  given RootJsonFormat[JsonWebKey2020] = jsonFormat1(JsonWebKey2020.apply)
  given RootJsonFormat[OperationProof] = jsonFormat3(OperationProof.apply)
  given RootJsonFormat[OperationProofSigned] = jsonFormat2(OperationProofSigned.apply)
  given RootJsonFormat[OperationType] = jsonFormat0(OperationType.apply)
  given RootJsonFormat[PublicKey] = jsonFormat5(PublicKey.apply)
  given RootJsonFormat[PublicKeyJwk] = jsonFormat5(PublicKeyJwk.apply)
  given RootJsonFormat[RecoverDIDRequest] = jsonFormat7(RecoverDIDRequest.apply)
  given RootJsonFormat[Service] = jsonFormat3(Service.apply)
  given RootJsonFormat[UpdateDIDRequest] = jsonFormat6(UpdateDIDRequest.apply)
  given RootJsonFormat[UpdatePatch] = jsonFormat2(UpdatePatch.apply)
  given RootJsonFormat[VerificationMethod] = jsonFormat4(VerificationMethod.apply)
  given RootJsonFormat[VerificationMethodOrRef] = jsonFormat2(VerificationMethodOrRef.apply)

}
