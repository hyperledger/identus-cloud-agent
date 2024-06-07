package org.hyperledger.identus.pollux.core.model.error

import org.hyperledger.identus.agent.walletapi.model.PublicationState
import org.hyperledger.identus.castor.core.model.did.{PrismDID, VerificationRelationship}
import org.hyperledger.identus.pollux.core.model.DidCommID
import org.hyperledger.identus.pollux.core.model.IssueCredentialRecord.ProtocolState
import org.hyperledger.identus.shared.models.{Failure, StatusCode}

import java.util.UUID

sealed trait CredentialServiceError

object CredentialServiceError {
  final case class RecordIdNotFound(recordId: DidCommID) extends CredentialServiceError
  final case class OperationNotExecuted(recordId: DidCommID, info: String) extends CredentialServiceError
  final case class InvalidFlowStateError(msg: String) extends CredentialServiceError
  final case class UnexpectedError(msg: String) extends CredentialServiceError
  final case class UnsupportedCredentialFormat(vcFormat: String) extends CredentialServiceError
  object MissingCredentialFormat extends CredentialServiceError
  final case class CreateCredentialPayloadFromRecordError(cause: Throwable) extends CredentialServiceError
  final case class CredentialRequestValidationError(error: String) extends CredentialServiceError
  final case class CredentialSchemaError(cause: org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError)
      extends CredentialServiceError
  final case class UnsupportedVCClaimsValue(error: String) extends CredentialServiceError
  final case class UnsupportedVCClaimsMediaType(media_type: String) extends CredentialServiceError
}

sealed trait CredentialServiceErrorNew(
    val statusCode: StatusCode,
    val userFacingMessage: String
) extends Failure
    with CredentialServiceError {
  override val namespace: String = "CredentialServiceError"
}

object CredentialServiceErrorNew {
  final case class InvalidCredentialOffer(cause: String)
      extends CredentialServiceErrorNew(
        StatusCode.BadRequest,
        s"The credential offer message is invalid: cause[$cause]"
      )

  final case class UnsupportedDidFormat(did: String)
      extends CredentialServiceErrorNew(
        StatusCode.UnprocessableContent,
        s"The DID format is not supported: did=$did"
      )

  final case class CredentialDefinitionServiceError(cause: String)
      extends CredentialServiceErrorNew(
        StatusCode.InternalServerError,
        s"An error occurred related the credential definition: cause[$cause]"
      )

  final case class CredentialDefinitionPrivatePartNotFound(guid: UUID)
      extends CredentialServiceErrorNew(
        StatusCode.NotFound,
        s"There is no private part matching the credential definition: guid=$guid"
      )

  final case class RecordNotFound(recordId: DidCommID, state: Option[ProtocolState])
      extends CredentialServiceErrorNew(
        StatusCode.NotFound,
        s"The requested record was not found: recordId=$recordId, state=$state"
      )

  final case class RecordNotFoundForThreadIdAndStates(thid: DidCommID, states: ProtocolState*)
      extends CredentialServiceErrorNew(
        StatusCode.NotFound,
        s"The requested record was not found: thid=${thid.value}, states:${states.mkString(",")}"
      )

  final case class DIDNotFoundInWallet(did: PrismDID)
      extends CredentialServiceErrorNew(
        StatusCode.NotFound,
        s"The requested DID does not exist in the wallet: did=${did.toString}"
      )

  final case class KeyPairNotFoundInWallet(did: PrismDID, keyId: String, algo: String)
      extends CredentialServiceErrorNew(
        StatusCode.NotFound,
        s"The requested key pair does not exist in the wallet: did=${did.toString}, keyId=$keyId, algo=$algo"
      )

  final case class DIDNotPublished(did: PrismDID, state: PublicationState)
      extends CredentialServiceErrorNew(
        StatusCode.UnprocessableContent,
        s"The DID must be published for this operation: did=${did.toString}, publicationState=$state"
      )

  final case class DIDNotResolved(did: PrismDID)
      extends CredentialServiceErrorNew(
        StatusCode.NotFound,
        s"The requested DID cannot be resolved: did=${did.toString}"
      )

  final case class KeyNotFoundInDID(did: PrismDID, verificationRelationship: VerificationRelationship)
      extends CredentialServiceErrorNew(
        StatusCode.NotFound,
        s"A key with the given purpose was not found in the DID: did=${did.toString}, purpose=${verificationRelationship.name}"
      )

  final case class InvalidCredentialRequest(cause: String)
    extends CredentialServiceErrorNew(
      StatusCode.BadRequest,
      s"The credential request message is invalid: cause[$cause]"
    )

  final case class InvalidCredentialIssue(cause: String)
    extends CredentialServiceErrorNew(
      StatusCode.BadRequest,
      s"The credential issue message is invalid: cause[$cause]"
    )

}
