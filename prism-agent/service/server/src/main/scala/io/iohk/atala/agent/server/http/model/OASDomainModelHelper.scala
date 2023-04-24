package io.iohk.atala.agent.server.http.model

import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.castor.core.model.did as castorDomain
import io.iohk.atala.agent.walletapi.model as walletDomain
import io.iohk.atala.pollux.core.model as polluxdomain
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.utils.Traverse.*

import io.lemonlabs.uri.Uri
import scala.util.Try
import java.time.OffsetDateTime
import java.time.ZoneOffset
import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.mercury.model.Base64
import zio.ZIO
import io.iohk.atala.agent.server.http.model.HttpServiceError.InvalidPayload
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.castor.core.model.did.w3c.PublicKeyRepr
import io.iohk.atala.castor.core.model.did.{LongFormPrismDID, PrismDID, ServiceType}

import java.util.UUID
import io.iohk.atala.castor.core.util.UriUtils

trait OASDomainModelHelper {

  extension (service: Service) {
    def toDomain: Either[String, castorDomain.Service] = {
      for {
        serviceEndpoint <- service.serviceEndpoint
          .getOrElse(Nil)
          .traverse(s => Uri.parseTry(s).toEither.left.map(_ => s"unable to parse serviceEndpoint $s as URI"))
        serviceType <- castorDomain.ServiceType
          .parseString(service.`type`)
          .toRight(s"unsupported serviceType ${service.`type`}")
      } yield castorDomain
        .Service(
          id = service.id,
          `type` = serviceType,
          serviceEndpoint = serviceEndpoint
        )
        .normalizeServiceEndpoint()
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

  extension (action: UpdateManagedDIDRequestActionsInner) {
    def toDomain: Either[String, walletDomain.UpdateManagedDIDAction] = {
      import walletDomain.UpdateManagedDIDAction.*
      action.actionType match {
        case "ADD_KEY" =>
          action.addKey
            .toRight("addKey property is missing from action type ADD_KEY")
            .flatMap(_.toDomain)
            .map(template => AddKey(template))
        case "REMOVE_KEY" =>
          action.removeKey
            .toRight("removeKey property is missing from action type REMOVE_KEY")
            .map(i => RemoveKey(i.id))
        case "ADD_SERVICE" =>
          action.addService
            .toRight("addservice property is missing from action type ADD_SERVICE")
            .flatMap(_.toDomain)
            .map(s => AddService(s))
        case "REMOVE_SERVICE" =>
          action.removeService
            .toRight("removeService property is missing from action type REMOVE_SERVICE")
            .map(i => RemoveService(i.id))
        case "UPDATE_SERVICE" =>
          action.updateService
            .toRight("updateService property is missing from action type UPDATE_SERVICE")
            .flatMap(_.toDomain)
            .map(s => UpdateService(s))
        case s => Left(s"unsupported update DID action type: $s")
      }
    }
  }

  extension (servicePatch: UpdateManagedDIDRequestActionsInnerUpdateService) {
    def toDomain: Either[String, walletDomain.UpdateServicePatch] =
      for {
        serviceEndpoint <- servicePatch.serviceEndpoint
          .getOrElse(Nil)
          .traverse(s => Uri.parseTry(s).toEither.left.map(_ => s"unable to parse serviceEndpoint $s as URI"))
        normalizedServiceEndpoint <- serviceEndpoint
          .traverse(uri =>
            UriUtils
              .normalizeUri(uri.toString)
              .toRight(s"unable to parse serviceEndpoint ${uri.toString} as URI")
              .map(Uri.parse)
          )
        serviceType <- servicePatch.`type`.fold[Either[String, Option[ServiceType]]](Right(None))(s =>
          castorDomain.ServiceType.parseString(s).toRight(s"unsupported serviceType $s").map(Some(_))
        )
      } yield walletDomain.UpdateServicePatch(
        id = servicePatch.id,
        serviceType = serviceType,
        serviceEndpoints = normalizedServiceEndpoint
      )
  }

  extension (publicKeyTemplate: ManagedDIDKeyTemplate) {
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
      recordId = domain.id.value,
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
      protocolState = domain.protocolState.toString(),
      jwtCredential = domain.issueCredentialData.flatMap(issueCredential => {
        issueCredential.attachments.collectFirst { case AttachmentDescriptor(_, _, Base64(jwt), _, _, _, _) =>
          jwt
        }
      })
    )
  }
  extension (domain: polluxdomain.PresentationRecord) {
    def toOAS: PresentationStatus = {
      val connectionId = domain.connectionId
      val data = domain.presentationData match
        case Some(p) =>
          p.attachments.head.data match {
            case Base64(data) =>
              val base64Decoded = new String(java.util.Base64.getDecoder().decode(data))
              println(s"Base64decode:\n\n ${base64Decoded} \n\n")
              Seq(base64Decoded)
            case any => ???
          }
        case None => Seq.empty
      PresentationStatus(
        presentationId = domain.id.toString,
        status = domain.protocolState.toString,
        proofs = Seq.empty,
        data = data,
        connectionId = connectionId
      )
    }
  }

  extension (str: String) {
    // FIXME REMOVE methods
    def toUUID: ZIO[Any, InvalidPayload, UUID] =
      ZIO
        .fromTry(Try(UUID.fromString(str)))
        .mapError(e => HttpServiceError.InvalidPayload(s"Error parsing string as UUID: ${e.getMessage()}"))

    def toDidCommID: ZIO[Any, InvalidPayload, io.iohk.atala.pollux.core.model.DidCommID] =
      ZIO
        .fromTry(Try(io.iohk.atala.pollux.core.model.DidCommID(str)))
        .mapError(e => HttpServiceError.InvalidPayload(s"Error parsing string as DidCommID: ${e.getMessage()}"))
  }

  extension (didDetail: walletDomain.ManagedDIDDetail) {
    def toOAS: ManagedDID = {
      val (longFormDID, status) = didDetail.state match {
        case ManagedDIDState.Created(operation) => Some(PrismDID.buildLongFormFromOperation(operation)) -> "CREATED"
        case ManagedDIDState.PublicationPending(operation, _) =>
          Some(PrismDID.buildLongFormFromOperation(operation)) -> "PUBLICATION_PENDING"
        case ManagedDIDState.Published(_, _) => None -> "PUBLISHED"
      }
      ManagedDID(
        did = didDetail.did.toString,
        longFormDid = longFormDID.map(_.toString),
        status = status
      )
    }
  }

}
