package io.iohk.atala.agent.server.http.marshaller

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.iohk.atala.agent.openapi.model.*
import spray.json.{DefaultJsonProtocol, RootJsonFormat, jsonReader}
import spray.json.JsonFormat
import java.util.UUID
import spray.json.JsString
import spray.json.JsValue
import spray.json.DeserializationException
import java.time.OffsetDateTime

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  // Castor
  given RootJsonFormat[AuthenticationChallengeSubmissionRequest] = jsonFormat3(
    AuthenticationChallengeSubmissionRequest.apply
  )
  given RootJsonFormat[AuthenticationChallengeSubmissionResponse] = jsonFormat2(
    AuthenticationChallengeSubmissionResponse.apply
  )
  given RootJsonFormat[CreateAuthenticationChallengeRequest] = jsonFormat3(CreateAuthenticationChallengeRequest.apply)
  given RootJsonFormat[CreateAuthenticationChallengeResponse] = jsonFormat2(CreateAuthenticationChallengeResponse.apply)
  given RootJsonFormat[CreateDIDRequest] = jsonFormat4(CreateDIDRequest.apply)
  given RootJsonFormat[CreateDIDRequestDocument] = jsonFormat2(CreateDIDRequestDocument.apply)
  given RootJsonFormat[CreateManagedDidRequest] = jsonFormat1(CreateManagedDidRequest.apply)
  given RootJsonFormat[CreateManagedDidRequestDocumentTemplate] = jsonFormat3(
    CreateManagedDidRequestDocumentTemplate.apply
  )
  given RootJsonFormat[CreateManagedDidRequestDocumentTemplatePublicKeysInner] = jsonFormat2(
    CreateManagedDidRequestDocumentTemplatePublicKeysInner.apply
  )
  given RootJsonFormat[CreateManagedDIDResponse] = jsonFormat1(CreateManagedDIDResponse.apply)
  given RootJsonFormat[DeactivateDIDRequest] = jsonFormat4(DeactivateDIDRequest.apply)
  given RootJsonFormat[Delta] = jsonFormat2(Delta.apply)
  given RootJsonFormat[DeltaUpdate] = jsonFormat2(DeltaUpdate.apply)
  given RootJsonFormat[DID] = jsonFormat8(DID.apply)
  given RootJsonFormat[DidOperation] = jsonFormat4(DidOperation.apply)
  given RootJsonFormat[DIDOperationResponse] = jsonFormat1(DIDOperationResponse.apply)
  given RootJsonFormat[DidOperationStatus] = jsonFormat0(DidOperationStatus.apply)
  given RootJsonFormat[DidOperationSubmission] = jsonFormat2(DidOperationSubmission.apply)
  given RootJsonFormat[DidOperationType] = jsonFormat0(DidOperationType.apply)
  given RootJsonFormat[DIDResponse] = jsonFormat2(DIDResponse.apply)
  given RootJsonFormat[ErrorResponse] = jsonFormat5(ErrorResponse.apply)
  given RootJsonFormat[JsonWebKey2020] = jsonFormat1(JsonWebKey2020.apply)
  given RootJsonFormat[PublicKey] = jsonFormat5(PublicKey.apply)
  given RootJsonFormat[PublicKeyJwk] = jsonFormat5(PublicKeyJwk.apply)
  given RootJsonFormat[RecoverDIDRequest] = jsonFormat5(RecoverDIDRequest.apply)
  given RootJsonFormat[Service] = jsonFormat3(Service.apply)
  given RootJsonFormat[UpdateDIDRequest] = jsonFormat4(UpdateDIDRequest.apply)
  given RootJsonFormat[UpdatePatch] = jsonFormat2(UpdatePatch.apply)
  given RootJsonFormat[VerificationMethod] = jsonFormat4(VerificationMethod.apply)
  given RootJsonFormat[VerificationMethodOrRef] = jsonFormat2(VerificationMethodOrRef.apply)

  // Issue Credential Protocol
  implicit object UUIDFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)
    def read(value: JsValue) = {
      value match {
        case JsString(uuid) => UUID.fromString(uuid)
        case _              => throw new DeserializationException("Expected hexadecimal UUID string")
      }
    }
  }
  implicit object OffsetDateTimeFormat extends JsonFormat[OffsetDateTime] {
    def write(dt: OffsetDateTime) = JsString(dt.toString)
    def read(value: JsValue) = {
      value match {
        case JsString(dt) => OffsetDateTime.parse(dt)
        case _            => throw new DeserializationException("Expected hexadecimal OffsetDateTime string")
      }
    }
  }
  given RootJsonFormat[CreateIssueCredentialRecordRequest] = jsonFormat6(CreateIssueCredentialRecordRequest.apply)
  given RootJsonFormat[IssueCredentialRecord] = jsonFormat13(IssueCredentialRecord.apply)
  given RootJsonFormat[IssueCredentialRecordCollection] = jsonFormat4(IssueCredentialRecordCollection.apply)
  //

  // Pollux
  given RootJsonFormat[CreateCredentials201Response] = jsonFormat3(CreateCredentials201Response.apply)
  given RootJsonFormat[CreateCredentialsRequest] = jsonFormat1(CreateCredentialsRequest.apply)
  given RootJsonFormat[RevocationStatus] = jsonFormat2(RevocationStatus.apply)
  given RootJsonFormat[W3CCredential] = jsonFormat6(W3CCredential.apply)
//  given RootJsonFormat[W3CCredentialCredentialSubject] = jsonFormat1(W3CCredentialCredentialSubject.apply)
  given RootJsonFormat[W3CCredentialInput] = jsonFormat4(W3CCredentialInput.apply)
//  given RootJsonFormat[W3CCredentialInputClaims] = jsonFormat1(W3CCredentialInputClaims.apply)
  given RootJsonFormat[W3CCredentialRevocationRequest] = jsonFormat1(W3CCredentialRevocationRequest.apply)
  given RootJsonFormat[W3CCredentialRevocationResponse] = jsonFormat2(W3CCredentialRevocationResponse.apply)
  given RootJsonFormat[W3CCredentialsPaginated] = jsonFormat4(W3CCredentialsPaginated.apply)
  given RootJsonFormat[W3CCredentialStatus] = jsonFormat2(W3CCredentialStatus.apply)
  given RootJsonFormat[W3CIssuanceBatch] = jsonFormat3(W3CIssuanceBatch.apply)
  given RootJsonFormat[W3CIssuanceBatchAction] = jsonFormat3(W3CIssuanceBatchAction.apply)
  given RootJsonFormat[W3CIssuanceBatchPaginated] = jsonFormat4(W3CIssuanceBatchPaginated.apply)
  // given RootJsonFormat[W3CPresentation] = jsonFormat1(W3CPresentation.apply)
  // given RootJsonFormat[W3CPresentationInput] = jsonFormat1(W3CPresentationInput.apply)
  // given RootJsonFormat[W3CPresentationPaginated] = jsonFormat4(W3CPresentationPaginated.apply)
  given RootJsonFormat[W3CProof] = jsonFormat6(W3CProof.apply)
  given RootJsonFormat[W3CSchema] = jsonFormat8(W3CSchema.apply)
  given RootJsonFormat[W3CSchemaAllOf] = jsonFormat2(W3CSchemaAllOf.apply)
  given RootJsonFormat[W3CSchemaClaims] = jsonFormat7(W3CSchemaClaims.apply)
  given RootJsonFormat[W3CSchemaClaimsProperties] = jsonFormat1(W3CSchemaClaimsProperties.apply)
  given RootJsonFormat[W3CSchemaInput] = jsonFormat4(W3CSchemaInput.apply)
  given RootJsonFormat[W3CSchemaMeta] = jsonFormat6(W3CSchemaMeta.apply)
  given RootJsonFormat[W3CSchemaPaginated] = jsonFormat4(W3CSchemaPaginated.apply)

  // Presentation
  given RootJsonFormat[ProofRequestAux] = jsonFormat2(ProofRequestAux.apply)
  given RootJsonFormat[RequestPresentationInput] = jsonFormat2(RequestPresentationInput.apply)
  given RootJsonFormat[RequestPresentationOutput] = jsonFormat1(RequestPresentationOutput.apply)
  given RootJsonFormat[PresentationStatus] = jsonFormat4(PresentationStatus.apply)
  given RootJsonFormat[RequestPresentationAction] = jsonFormat2(RequestPresentationAction.apply)

  // Connections Management
  given RootJsonFormat[CreateConnectionRequest] = jsonFormat1(CreateConnectionRequest.apply)
  given RootJsonFormat[AcceptConnectionInvitationRequest] = jsonFormat1(AcceptConnectionInvitationRequest.apply)
  given RootJsonFormat[ConnectionCollection] = jsonFormat3(ConnectionCollection.apply)
  given RootJsonFormat[Connection] = jsonFormat10(Connection.apply)
  given RootJsonFormat[ConnectionInvitation] = jsonFormat4(ConnectionInvitation.apply)

}
