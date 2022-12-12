package io.iohk.atala.agent.server.http.model

import io.iohk.atala.agent.openapi.model.{
  Connection,
  ConnectionInvitation,
  CreateManagedDidRequestDocumentTemplate,
  CreateManagedDidRequestDocumentTemplatePublicKeysInner,
  DID,
  DIDDocumentMetadata,
  DIDOperationResponse,
  DIDResponse,
  DidOperationSubmission,
  IssueCredentialRecord,
  IssueCredentialRecordCollection,
  ListManagedDIDResponseInner,
  PublicKeyJwk,
  Service,
  VerificationMethod
}
import io.iohk.atala.castor.core.model.did as castorDomain
import io.iohk.atala.agent.walletapi.model as walletDomain
import io.iohk.atala.pollux.core.model as polluxdomain
import io.iohk.atala.connect.core.model as connectdomain
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.utils.Traverse.*

import java.net.URI
import scala.util.Try
import java.time.OffsetDateTime
import java.time.ZoneOffset
import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.mercury.model.Base64
import zio.ZIO
import io.iohk.atala.agent.server.http.model.HttpServiceError.InvalidPayload
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.castor.core.model.did.{LongFormPrismDID, PrismDID}

import java.util.UUID
import io.iohk.atala.connect.core.model.ConnectionRecord.Role

trait OASDomainModelHelper {

  extension (service: Service) {
    def toDomain: Either[String, castorDomain.Service] = {
      for {
        serviceEndpoint <- service.serviceEndpoint.traverse(s =>
          Try(URI.create(s)).toEither.left.map(_ => s"unable to parse serviceEndpoint $s as URI")
        )
        serviceType <- castorDomain.ServiceType
          .parseString(service.`type`)
          .toRight(s"unsupported serviceType ${service.`type`}")
      } yield castorDomain.Service(
        id = service.id,
        `type` = serviceType,
        serviceEndpoint = serviceEndpoint
      )
    }
  }

  extension (template: CreateManagedDidRequestDocumentTemplate) {
    def toDomain: Either[String, walletDomain.ManagedDIDTemplate] = {
      for {
        services <- template.services.traverse(_.toDomain)
        publicKeys <- template.publicKeys.traverse(_.toDomain)
      } yield walletDomain.ManagedDIDTemplate(
        publicKeys = publicKeys,
        services = services
      )
    }
  }

  extension (publicKeyTemplate: CreateManagedDidRequestDocumentTemplatePublicKeysInner) {
    def toDomain: Either[String, walletDomain.DIDPublicKeyTemplate] = {
      for {
        purpose <- castorDomain.VerificationRelationship
          .parseString(publicKeyTemplate.purpose)
          .toRight(s"unsupported verificationRelationship ${publicKeyTemplate.purpose}")
      } yield walletDomain.DIDPublicKeyTemplate(
        id = publicKeyTemplate.id,
        purpose = purpose
      )
    }
  }

  extension (outcome: castorDomain.ScheduleDIDOperationOutcome) {
    def toOAS: DIDOperationResponse = DIDOperationResponse(
      scheduledOperation = DidOperationSubmission(
        id = HexString.fromByteArray(outcome.operationId.toArray).toString,
        didRef = outcome.did.toString
      )
    )
  }

  extension (domain: polluxdomain.IssueCredentialRecord) {
    def toOAS: IssueCredentialRecord = IssueCredentialRecord(
      recordId = domain.id,
      createdAt = domain.createdAt.atOffset(ZoneOffset.UTC),
      updatedAt = domain.updatedAt.map(_.atOffset(ZoneOffset.UTC)),
      role = domain.role.toString,
      subjectId = domain.subjectId,
      claims = domain.offerCredentialData
        .map(offer => offer.body.credential_preview.attributes.map(attr => (attr.name -> attr.value)).toMap)
        .getOrElse(Map.empty),
      schemaId = domain.schemaId,
      validityPeriod = domain.validityPeriod,
      automaticIssuance = domain.automaticIssuance,
      awaitConfirmation = domain.awaitConfirmation,
      protocolState = domain.protocolState.toString(),
      publicationState = domain.publicationState.map(_.toString),
      jwtCredential = domain.issueCredentialData.flatMap(issueCredential => {
        issueCredential.attachments.collectFirst { case AttachmentDescriptor(_, _, Base64(jwt), _, _, _, _) =>
          jwt
        }
      })
    )
  }
  extension (domain: connectdomain.ConnectionRecord) {
    def toOAS: Connection = Connection(
      label = domain.label,
      self = "Connection",
      kind = s"/connections/${domain.id.toString}",
      connectionId = domain.id,
      myDid = domain.role match
        case Role.Inviter =>
          domain.connectionResponse.map(_.from).orElse(domain.connectionRequest.map(_.to)).map(_.value)
        case Role.Invitee =>
          domain.connectionResponse.map(_.to).orElse(domain.connectionRequest.map(_.from)).map(_.value)
      ,
      theirDid = domain.role match
        case Role.Inviter =>
          domain.connectionResponse.map(_.to).orElse(domain.connectionRequest.map(_.from)).map(_.value)
        case Role.Invitee =>
          domain.connectionResponse.map(_.from).orElse(domain.connectionRequest.map(_.to)).map(_.value)
      ,
      state = domain.protocolState.toString,
      createdAt = domain.createdAt.atOffset(ZoneOffset.UTC),
      updatedAt = domain.updatedAt.map(_.atOffset(ZoneOffset.UTC)),
      invitation = ConnectionInvitation(
        id = UUID.fromString(domain.invitation.id),
        `type` = domain.invitation.`type`,
        from = domain.invitation.from.value,
        invitationUrl = s"https://domain.com/path?_oob=${domain.invitation.toBase64}"
      )
    )
  }

  extension (str: String) {
    def toUUID: ZIO[Any, InvalidPayload, UUID] =
      ZIO
        .fromTry(Try(UUID.fromString(str)))
        .mapError(e => HttpServiceError.InvalidPayload(s"Error parsing string as UUID: ${e.getMessage()}"))
  }

  extension (resolution: (castorDomain.w3c.DIDDocumentMetadataRepr, castorDomain.w3c.DIDDocumentRepr)) {
    def toOAS: DIDResponse = {
      val (metadata, didDoc) = resolution
      DIDResponse(
        did = DID(
          id = didDoc.id,
          controller = Some(didDoc.controller),
          verificationMethod = Some(didDoc.verificationMethod.map(_.toOAS)),
          authentication = Some(didDoc.authentication.map(_.toOAS)),
          assertionMethod = Some(didDoc.assertionMethod.map(_.toOAS)),
          keyAgreement = Some(didDoc.keyAgreement.map(_.toOAS)),
          capabilityInvocation = Some(didDoc.capabilityInvocation.map(_.toOAS)),
          service = Some(didDoc.service.map(_.toOAS))
        ),
        metadata = DIDDocumentMetadata(deactivated = metadata.deactivated)
      )
    }
  }

  extension (publicKeyRepr: castorDomain.w3c.PublicKeyRepr) {
    def toOAS: VerificationMethod = {
      VerificationMethod(
        id = publicKeyRepr.id,
        `type` = publicKeyRepr.`type`,
        controller = publicKeyRepr.controller,
        publicKeyJwk = publicKeyRepr.publicKeyJwk.toOAS
      )
    }
  }

  extension (publicKeyJwk: castorDomain.w3c.PublicKeyJwk) {
    def toOAS: PublicKeyJwk = {
      PublicKeyJwk(
        crv = Some(publicKeyJwk.crv),
        x = Some(publicKeyJwk.x),
        y = Some(publicKeyJwk.y),
        kty = publicKeyJwk.kty,
        kid = None
      )
    }
  }

  extension (service: castorDomain.w3c.ServiceRepr) {
    def toOAS: Service = Service(
      id = service.id,
      `type` = service.`type`,
      serviceEndpoint = service.serviceEndpoint // FIXME @pat
    )
  }

  extension (didDetail: walletDomain.ManagedDIDDetail) {
    def toOAS: ListManagedDIDResponseInner = {
      val (longFormDID, status) = didDetail.state match {
        case ManagedDIDState.Created(operation) => Some(PrismDID.buildLongFormFromOperation(operation)) -> "CREATED"
        case ManagedDIDState.PublicationPending(operation, _) =>
          Some(PrismDID.buildLongFormFromOperation(operation)) -> "PUBLICATION_PENDING"
        case ManagedDIDState.Published(_, _) => None -> "PUBLISHED"
      }
      ListManagedDIDResponseInner(
        did = didDetail.did.toString,
        longFormDid = longFormDID.map(_.toString),
        status = status
      )
    }
  }

}
