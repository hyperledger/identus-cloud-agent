package io.iohk.atala.agent.server.http.model

import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.castor.core.model.did as castorDomain
import io.iohk.atala.agent.walletapi.model as walletDomain
import io.iohk.atala.pollux.core.model as polluxdomain
import io.iohk.atala.shared.models.{HexString, Base64UrlString}
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
