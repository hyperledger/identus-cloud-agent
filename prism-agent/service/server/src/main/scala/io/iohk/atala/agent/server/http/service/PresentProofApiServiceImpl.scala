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
  def getAllPresentation()(implicit
      toEntityMarshallerPresentationStatus: ToEntityMarshaller[PresentationStatus]
  ): Route = {
    // val presentationService: PresentationService = ??? // TODO FIXME

    // val result = for {
    //   outcome <- presentationService.getPresentationRecords()
    //   // .mapError(HttpServiceError.DomainError[PresentationError.UnexpectedError].apply)
    // } yield outcome

    // onZioSuccess(result) { e =>
    //   getAllPresentation200(e)
    // }
    ??? // FIXME
  }

  def requestPresentation(requestPresentationInput: RequestPresentationInput)(implicit
      toEntityMarshallerRequestPresentationOutput: ToEntityMarshaller[RequestPresentationOutput]
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
      result: RequestPresentationOutput = RequestPresentationOutput(record.id.toString)
    } yield result

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error) => complete(error.status -> error)
      case Right(result) =>
        requestPresentation201(
          IssueCredentialRecordCollection(
            items = result,
            offset = 0,
            limit = 0,
            count = result.size
          )
        )
    }

    onZioSuccess(result) { e =>
      requestPresentation201(e)
    }
  }

  def sendPresentation(
      id: String,
      sendPresentationInput: SendPresentationInput
  ): Route = ???
}

object PresentProofApiServiceImpl {
  val layer: URLayer[Any, PresentProofApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      // svc <- ZIO.service[PresentationService]
    } yield PresentProofApiServiceImpl /*(svc)*/ (using rt)
  }
}
