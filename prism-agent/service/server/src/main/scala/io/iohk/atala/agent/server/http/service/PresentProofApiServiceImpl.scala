package io.iohk.atala.agent.server.http.service

import io.iohk.atala.agent.openapi.api.PresentProofApiService
import io.iohk.atala.agent.openapi.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import zio._
import scala.concurrent.Future
import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.mercury._
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.presentproof._
import java.util.UUID
import io.iohk.atala.agent.server.http.model.OASDomainModelHelper
import io.iohk.atala.agent.server.http.model.OASErrorModelHelper
import io.iohk.atala.agent.server.http.model.InvalidState
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.agent.server.http.model.HttpServiceError.DomainError

import io.iohk.atala.pollux.vc.jwt.Issuer
import io.iohk.atala.pollux.core.service.PresentationService
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model._
import io.iohk.atala.mercury.model.Base64
import cats.instances.option
import io.iohk.atala.pollux.core.model.presentation.Options
import io.iohk.atala.agent.openapi.model.PublicKeyJwk

class PresentProofApiServiceImpl(presentationService: PresentationService, connectionService: ConnectionService)(using
    runtime: Runtime[Any]
) extends PresentProofApiService
    with AkkaZioSupport
    with OASDomainModelHelper
    with OASErrorModelHelper {

  override def requestPresentation(requestPresentationInput: RequestPresentationInput)(implicit
      toEntityMarshallerRequestPresentationOutput: ToEntityMarshaller[RequestPresentationOutput],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      didId <- connectionService
        .getConnectionRecord(UUID.fromString(requestPresentationInput.connectionId))
        .map(_.flatMap(_.connectionRequest).map(_.from).get) // TODO GET
        .mapError(HttpServiceError.DomainError[ConnectionServiceError].apply)
        .mapError(_.toOAS)
      record <- presentationService
        .createPresentationRecord(
          thid = DidCommID(),
          subjectDid = didId,
          connectionId = Some(requestPresentationInput.connectionId),
          proofTypes = requestPresentationInput.proofs.map { e =>
            ProofType(
              schema = e.schemaId, // TODO rename field to schemaId
              requiredFields = None,
              trustIssuers = Some(e.trustIssuers.map(DidId(_)))
            )
          },
          options = requestPresentationInput.options.map(x => Options(x.challenge, x.domain))
        )
        .mapError(HttpServiceError.DomainError[PresentationError].apply)
        .mapError(_.toOAS)
    } yield RequestPresentationOutput(record.id.toString)

    onZioSuccess(result.either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => requestPresentation201(result)
    }

  }

  override def getAllPresentation(
      offset: Option[Int],
      limit: Option[Int],
      thid: Option[String]
  )(implicit
      toEntityMarshallerPresentationStatusPage: ToEntityMarshaller[PresentationStatusPage],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      records <- presentationService
        .getPresentationRecords()
        .mapError(HttpServiceError.DomainError[PresentationError].apply)
      outcome = thid match
        case None        => records
        case Some(value) => records.filter(_.thid.value == value) // this logic should be moved to the DB
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error) => complete(error.status -> error)
      case Right(results) =>
        getAllPresentation200(
          PresentationStatusPage(
            self = "/present-proof/presentations",
            kind = "Collection",
            pageOf = "1",
            next = None,
            previous = None,
            contents = results
          )
        )
    }
  }

  def getPresentation(id: String)(implicit
      toEntityMarshallerPresentationStatus: ToEntityMarshaller[PresentationStatus],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      presentationId <- id.toDidCommID
      outcome <- presentationService
        .getPresentationRecord(presentationId)
        .mapError(HttpServiceError.DomainError[PresentationError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error)         => complete(error.status -> error)
      case Right(Some(result)) => getPresentation200(result)
      case Right(None)         => getPresentation404(notFoundErrorResponse(Some("Presentation record not found")))
    }
  }

  override def updatePresentation(id: String, requestPresentationAction: RequestPresentationAction)(implicit
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = requestPresentationAction.action match {

      case "request-accept" =>
        for {
          record <- presentationService
            .acceptRequestPresentation(
              recordId = DidCommID(id),
              crecentialsToUse = requestPresentationAction.proofId.getOrElse(Seq.empty)
            )
            .mapError(HttpServiceError.DomainError[PresentationError].apply)
        } yield record
      case "request-reject" => {
        for {
          record <- presentationService
            .rejectRequestPresentation(DidCommID(id))
            .mapError(HttpServiceError.DomainError[PresentationError].apply)
        } yield record
      }
      case "presentation-accept" =>
        for {
          record <- presentationService
            .acceptPresentation(DidCommID(id))
            .mapError(HttpServiceError.DomainError[PresentationError].apply)
        } yield record // TODO FIXME
      case "presentation-reject" => {
        for {
          record <- presentationService
            .rejectPresentation(DidCommID(id))
            .mapError(HttpServiceError.DomainError[PresentationError].apply)
        } yield record
      }
      case s =>
        throw InvalidState(
          s"Error: updatePresentation's State must be 'request-accept','request-reject', 'presentation-accept' or 'presentation-reject' but is $s"
        )
    }

    onZioSuccess(result.mapBoth(_.toOAS, record => record).either) {
      case Left(error) => complete(error.status -> error)
      case Right(_)    => updatePresentation200
    }
  }
}

object PresentProofApiServiceImpl {
  val layer: URLayer[PresentationService & ConnectionService, PresentProofApiService] = ZLayer.fromZIO {

    for {
      rt <- ZIO.runtime[Any]
      presentationService <- ZIO.service[PresentationService]
      connectionService <- ZIO.service[ConnectionService]
    } yield PresentProofApiServiceImpl(presentationService, connectionService)(using rt)
  }
}
