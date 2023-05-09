package io.iohk.atala.agent.server.http.model

import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.agent.server.http.model.HttpServiceError.InvalidPayload
import io.iohk.atala.agent.walletapi.model as walletDomain
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.castor.core.model.did as castorDomain
import io.iohk.atala.castor.core.model.did.w3c.PublicKeyRepr
import io.iohk.atala.castor.core.model.did.{LongFormPrismDID, PrismDID, ServiceType}
import io.iohk.atala.castor.core.util.UriUtils
import io.iohk.atala.mercury.model.{AttachmentDescriptor, Base64}
import io.iohk.atala.pollux.core.model as polluxdomain
import io.iohk.atala.shared.models.{HexString, Base64UrlString}
import io.iohk.atala.shared.utils.Traverse.*
import io.lemonlabs.uri.Uri
import spray.json.{JsObject, JsString, JsonParser}
import zio.ZIO

import java.nio.charset.StandardCharsets
import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.util.Try

trait OASDomainModelHelper {

  extension (service: Service) {
    def toDomain: Either[String, castorDomain.Service] = {
      for {
        serviceEndpoint <- service.serviceEndpoint
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

  extension (domain: polluxdomain.IssueCredentialRecord) {
    def toOAS: IssueCredentialRecord = IssueCredentialRecord(
      recordId = domain.id.value,
      createdAt = domain.createdAt.atOffset(ZoneOffset.UTC),
      updatedAt = domain.updatedAt.map(_.atOffset(ZoneOffset.UTC)),
      role = domain.role.toString,
      subjectId = domain.subjectId,
      claims = domain.offerCredentialData
        .map(offer =>
          offer.body.credential_preview.attributes
            .foldLeft(JsObject()) { case (jsObject, attr) =>
              val jsonValue = attr.mimeType match
                case Some("application/json") =>
                  val jsonBytes = java.util.Base64.getUrlDecoder.decode(attr.value.getBytes(StandardCharsets.UTF_8))
                  JsonParser(jsonBytes)
                case Some(mime) => JsString(s"Unsupported 'mime-type': $mime")
                case None       => JsString(attr.value)
              jsObject.copy(fields = jsObject.fields + (attr.name -> jsonValue))
            }
        )
        .getOrElse(JsObject()),
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
