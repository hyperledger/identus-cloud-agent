package io.iohk.atala.agent.server.http.service

import io.iohk.atala.agent.openapi.api.PresentProofApiService
import io.iohk.atala.agent.openapi.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route

import zio._
import scala.concurrent.Future
import io.iohk.atala.agent.server.http.model.HttpServiceError

class PresentationService {
  def getAllPresentationRecord: UIO[PresentationStatus] = ??? // TODO FIXME
}

sealed trait PresentationError

object PresentationError {
  final case class UnexpectedError(msg: String) extends PresentationError
}

class PresentProofApiServiceImpl(using runtime: Runtime[Any]) extends PresentProofApiService with AkkaZioSupport {
  def getAllPresentation()(implicit
      toEntityMarshallerPresentationStatus: ToEntityMarshaller[PresentationStatus]
  ): Route = {
    val presentationService: PresentationService = ??? // TODO FIXME

    val result = for {
      outcome <- presentationService.getAllPresentationRecord
      // .mapError(HttpServiceError.DomainError[PresentationError.UnexpectedError].apply)
    } yield outcome

    onZioSuccess(result) { e =>
      getAllPresentation200(e)
    }
  }

  def requestPresentation(requestPresentationInput: RequestPresentationInput)(implicit
      toEntityMarshallerRequestPresentationOutput: ToEntityMarshaller[RequestPresentationOutput]
  ): Route = ???

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
