package io.iohk.atala.agent.server.http.marshaller

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.iohk.atala.agent.openapi.model.*
import spray.json.{
  DefaultJsonProtocol,
  DeserializationException,
  JsString,
  JsValue,
  JsonFormat,
  RootJsonFormat,
  jsonReader,
  jsonWriter
}

import java.util.UUID
import java.time.OffsetDateTime

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  // Castor
  given RootJsonFormat[CreateManagedDidRequest] = jsonFormat1(CreateManagedDidRequest.apply)
  given RootJsonFormat[CreateManagedDidRequestDocumentTemplate] = jsonFormat2(
    CreateManagedDidRequestDocumentTemplate.apply
  )
  given RootJsonFormat[CreateManagedDIDResponse] = jsonFormat1(CreateManagedDIDResponse.apply)
  given RootJsonFormat[DID] = jsonFormat9(DID.apply)
  given RootJsonFormat[DIDDocumentMetadata] = jsonFormat2(DIDDocumentMetadata.apply)
  given RootJsonFormat[DIDOperationResponse] = jsonFormat1(DIDOperationResponse.apply)
  given RootJsonFormat[DidOperationSubmission] = jsonFormat2(DidOperationSubmission.apply)
  given RootJsonFormat[DIDResponse] = jsonFormat2(DIDResponse.apply)
  given RootJsonFormat[ErrorResponse] = jsonFormat5(ErrorResponse.apply)
  given RootJsonFormat[ListManagedDIDResponseInner] = jsonFormat3(ListManagedDIDResponseInner.apply)
  given RootJsonFormat[ManagedDIDKeyTemplate] = jsonFormat2(ManagedDIDKeyTemplate.apply)
  given RootJsonFormat[PublicKeyJwk] = jsonFormat5(PublicKeyJwk.apply)
  given RootJsonFormat[Service] = jsonFormat3(Service.apply)
  given RootJsonFormat[UpdateManagedDIDRequest] = jsonFormat1(UpdateManagedDIDRequest.apply)
  given RootJsonFormat[UpdateManagedDIDRequestActionsInner] = jsonFormat6(UpdateManagedDIDRequestActionsInner.apply)
  given RootJsonFormat[UpdateManagedDIDRequestActionsInnerRemoveKey] = jsonFormat1(
    UpdateManagedDIDRequestActionsInnerRemoveKey.apply
  )
  given RootJsonFormat[UpdateManagedDIDRequestActionsInnerRemoveService] = jsonFormat1(
    UpdateManagedDIDRequestActionsInnerRemoveService.apply
  )
  given RootJsonFormat[UpdateManagedDIDRequestActionsInnerUpdateService] = jsonFormat3(
    UpdateManagedDIDRequestActionsInnerUpdateService.apply
  )
  given RootJsonFormat[VerificationMethod] = jsonFormat4(VerificationMethod.apply)
  given RootJsonFormat[VerificationMethodOrRef] = jsonFormat3(VerificationMethodOrRef.apply)

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
  // Issue
  given RootJsonFormat[CreateIssueCredentialRecordRequest] = jsonFormat7(CreateIssueCredentialRecordRequest.apply)
  given RootJsonFormat[IssueCredentialRecord] = jsonFormat12(IssueCredentialRecord.apply)
  given RootJsonFormat[IssueCredentialRecordCollection] = jsonFormat1(IssueCredentialRecordCollection.apply)

  // Presentation
  given RootJsonFormat[Options] = jsonFormat2(Options.apply)
  given RootJsonFormat[ProofRequestAux] = jsonFormat2(ProofRequestAux.apply)
  given RootJsonFormat[RequestPresentationInput] = jsonFormat3(RequestPresentationInput.apply)
  given RootJsonFormat[RequestPresentationOutput] = jsonFormat1(RequestPresentationOutput.apply)
  given RootJsonFormat[PresentationStatus] = jsonFormat5(PresentationStatus.apply)
  given RootJsonFormat[RequestPresentationAction] = jsonFormat2(RequestPresentationAction.apply)

  // Connections Management
  given RootJsonFormat[CreateConnectionRequest] = jsonFormat1(CreateConnectionRequest.apply)
  given RootJsonFormat[AcceptConnectionInvitationRequest] = jsonFormat1(AcceptConnectionInvitationRequest.apply)
  given RootJsonFormat[ConnectionCollection] = jsonFormat3(ConnectionCollection.apply)
  given RootJsonFormat[Connection] = jsonFormat11(Connection.apply)
  given RootJsonFormat[ConnectionInvitation] = jsonFormat4(ConnectionInvitation.apply)

}
