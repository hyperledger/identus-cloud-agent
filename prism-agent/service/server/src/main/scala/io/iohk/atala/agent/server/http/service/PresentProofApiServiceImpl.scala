package io.iohk.atala.agent.server.http.service

import io.iohk.atala.agent.openapi.api.PresentProofApiService
import io.iohk.atala.agent.openapi.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route

import zio._
import scala.concurrent.Future
import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.pollux.core.service.PresentationService
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.presentproof._
import java.util.UUID
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.agent.server.http.model.OASDomainModelHelper
import io.iohk.atala.agent.server.http.model.OASErrorModelHelper

class ConnectionService {
  // def getConnection(connectionId: String): UIO[DidId] = ???
  def getEstablishedConnection(connectionId: String): UIO[DidId] = ???
}

class PresentProofApiServiceImpl(
    presentationService: PresentationService,
    connectionService: ConnectionService,
    didCommService: DidComm
)(using runtime: Runtime[Any])
    extends PresentProofApiService
    with AkkaZioSupport
    with OASDomainModelHelper
    with OASErrorModelHelper {

  override def requestPresentation(requestPresentationInput: RequestPresentationInput)(implicit
      toEntityMarshallerRequestPresentationOutput: ToEntityMarshaller[RequestPresentationOutput],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {

    val result = for {
      toDID <- connectionService.getEstablishedConnection(requestPresentationInput.connectionId)

      record <- presentationService
        .createPresentationRecord(
          thid = UUID.randomUUID(),
          subjectDid = toDID,
          schemaId = None
        )
        .mapError(HttpServiceError.DomainError[PresentationError].apply)
    } yield record

    onZioSuccess(result.mapBoth(_.toOAS, record => RequestPresentationOutput(record.id.toString)).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => requestPresentation201(result)
    }

  }

  override def getAllPresentation()(implicit
      toEntityMarshallerPresentationStatusarray: ToEntityMarshaller[Seq[PresentationStatus]],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {

    val result = for {
      record <- presentationService
        .getPresentationRecords()
        .mapError(HttpServiceError.DomainError[PresentationError].apply)
    } yield record

    onZioSuccess(result.mapBoth(_.toOAS, record => record).either) {
      case Left(error) => complete(error.status -> error)
      case Right(result) => {
        // TODO map this correctly Presentation Model
        getAllPresentation200(Seq(PresentationStatus("111", "DD", Seq.empty, None)))
      }
    }
  }

  override def updatePresentation(id: String, updatePresentationRequest: UpdatePresentationRequest)(implicit
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      // action accept , decline
      // if accept
      record <- presentationService
        .acceptRequestPresentation(UUID.fromString(id))
        .mapError(HttpServiceError.DomainError[PresentationError].apply)
    } yield record

    onZioSuccess(result.mapBoth(_.toOAS, record => record).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => updatePresentation200
    }
  }
}

object PresentProofApiServiceImpl {
  val layer: URLayer[PresentationService & ConnectionService & DidComm, PresentProofApiService] = ZLayer.fromZIO {

    for {
      rt <- ZIO.runtime[Any]
      presentationService <- ZIO.service[PresentationService]
      connectionService <- ZIO.service[ConnectionService]
      didCommService <- ZIO.service[DidComm]
    } yield PresentProofApiServiceImpl(presentationService, connectionService, didCommService)(using rt)
  }
}
