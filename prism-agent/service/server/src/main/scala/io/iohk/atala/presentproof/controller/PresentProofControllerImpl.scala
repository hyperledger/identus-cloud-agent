package io.iohk.atala.presentproof.controller
import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.connect.controller.ConnectionController
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.presentproof.ProofType
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.presentation.Options
import io.iohk.atala.pollux.core.model.{DidCommID, PresentationRecord}
import io.iohk.atala.pollux.core.service.PresentationService
import io.iohk.atala.presentproof.controller.PresentProofController.toDidCommID
import io.iohk.atala.presentproof.controller.http.*
import zio.{IO, ZIO}

import java.util.UUID

class PresentProofControllerImpl(
    presentationService: PresentationService,
    connectionService: ConnectionService
) extends PresentProofController {
  override def requestPresentation(requestPresentationInput: RequestPresentationInput)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, RequestPresentationOutput] = {
    val result: IO[ConnectionServiceError | PresentationError, RequestPresentationOutput] = for {
      didId <- connectionService
        .getConnectionRecord(UUID.fromString(requestPresentationInput.connectionId))
        .map(_.flatMap(_.connectionRequest).map(_.from).get) // TODO GET
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
          options = requestPresentationInput.options.map(x => Options(x.challenge, x.domain)),
        )
    } yield RequestPresentationOutput.fromDomain(record)

    result.mapError {
      case e: ConnectionServiceError => ConnectionController.toHttpError(e)
      case e: PresentationError      => PresentProofController.toHttpError(e)
    }
  }

  override def getAllPresentation(offset: Option[Int], limit: Option[Int], thid: Option[String])(implicit
      rc: RequestContext
  ): IO[ErrorResponse, PresentationStatusPage] = {
    val result = for {
      records <- presentationService.getPresentationRecords()
      filteredRecords = thid match
        case None        => records
        case Some(value) => records.filter(_.thid.value == value) // this logic should be moved to the DB
    } yield PresentationStatusPage(
      filteredRecords.map(PresentationStatus.fromDomain)
    )

    result.mapError(PresentProofController.toHttpError)
  }

  override def getPresentation(id: String)(implicit rc: RequestContext): IO[ErrorResponse, PresentationStatus] = {
    val result: ZIO[Any, ErrorResponse | PresentationError, PresentationStatus] = for {
      presentationId <- toDidCommID(id)
      maybeRecord <- presentationService.getPresentationRecord(presentationId)
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => ErrorResponse.notFound(detail = Some(s"Presentation record not found: $id")))
    } yield PresentationStatus.fromDomain(record)

    result.mapError {
      case e: ErrorResponse     => e
      case e: PresentationError => PresentProofController.toHttpError(e)
    }
  }

  override def updatePresentation(id: String, requestPresentationAction: RequestPresentationAction)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, PresentationStatus] = {
    val result: ZIO[Any, ErrorResponse | PresentationError, PresentationStatus] = for {
      maybeRecord <- requestPresentationAction.action match {
        case "request-accept" =>
          presentationService.acceptRequestPresentation(
            recordId = DidCommID(id),
            crecentialsToUse = requestPresentationAction.proofId.getOrElse(Seq.empty)
          )
        case "request-reject"      => presentationService.rejectRequestPresentation(DidCommID(id))
        case "presentation-accept" => presentationService.acceptPresentation(DidCommID(id))
        case "presentation-reject" => presentationService.rejectPresentation(DidCommID(id))
        case a =>
          ZIO.fail(
            ErrorResponse.badRequest(
              detail = Some(
                s"presentation action must be 'request-accept','request-reject', 'presentation-accept', or 'presentation-reject' but is $a"
              )
            )
          )
      }
      record <- ZIO
        .fromOption(maybeRecord)
        .mapError(_ => ErrorResponse.notFound(detail = Some(s"Presentation record not found: $id")))

    } yield PresentationStatus.fromDomain(record)

    result.mapError {
      case e: ErrorResponse     => e
      case e: PresentationError => PresentProofController.toHttpError(e)
    }
  }
}
