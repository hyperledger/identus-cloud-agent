package io.iohk.atala.castor.controller

import io.iohk.atala.castor.controller.http.DIDResolutionResult
import sttp.model.StatusCode
import zio.*
import io.iohk.atala.castor.controller.http.{DIDDocument, DIDDocumentMetadata, DIDResolutionMetadata}
import io.iohk.atala.castor.core.model.did.w3c.{DIDDocumentMetadataRepr, DIDDocumentRepr, DIDResolutionErrorRepr}
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.model.did.w3c.makeW3CResolver
import io.iohk.atala.castor.controller.DIDControllerImpl.resolutionStatusCodeMapping
import scala.language.implicitConversions

trait DIDController {
  def getDID(did: String): UIO[(StatusCode, DIDResolutionResult)]
}

object DIDController {
  def toResolutionResult(
      didDocumentMetadata: DIDDocumentMetadataRepr,
      didDocument: DIDDocumentRepr
  ): DIDResolutionResult = {
    val isDeactivated = didDocumentMetadata.deactivated
    DIDResolutionResult(
      `@context` = "https://w3id.org/did-resolution/v1",
      didDocument = if (isDeactivated) None else Some(didDocument),
      didDocumentMetadata = didDocumentMetadata,
      didResolutionMetadata = DIDResolutionMetadata()
    )
  }

  def toResolutionResult(resolutionError: DIDResolutionErrorRepr): DIDResolutionResult = {
    DIDResolutionResult(
      `@context` = "https://w3id.org/did-resolution/v1",
      didDocument = None,
      didDocumentMetadata = DIDDocumentMetadata(),
      didResolutionMetadata = DIDResolutionMetadata(
        error = Some(resolutionError.value),
        errorMessage = resolutionError.errorMessage
      )
    )
  }
}

class DIDControllerImpl(service: DIDService) extends DIDController {

  override def getDID(did: String): UIO[(StatusCode, DIDResolutionResult)] = {
    for {
      result <- makeW3CResolver(service)(did).either
      resolutionResult = result.fold(
        DIDController.toResolutionResult,
        { case (metadata, document) =>
          DIDController.toResolutionResult(metadata, document)
        }
      )
      statusCode = resolutionStatusCodeMapping(resolutionResult, result.swap.toOption)
    } yield statusCode -> resolutionResult
  }

}

object DIDControllerImpl {
  val layer: URLayer[DIDService, DIDController] = ZLayer.fromFunction(DIDControllerImpl(_))

  // MUST conform to https://w3c-ccg.github.io/did-resolution/#bindings-https
  def resolutionStatusCodeMapping(
      resolutionResult: DIDResolutionResult,
      resolutionError: Option[DIDResolutionErrorRepr]
  ): StatusCode = {
    import DIDResolutionErrorRepr.*
    val isDeactivated = resolutionResult.didDocumentMetadata.deactivated.getOrElse(false)
    resolutionError match {
      case None if !isDeactivated           => StatusCode.Ok
      case None                             => StatusCode.Gone
      case Some(InvalidDID(_))              => StatusCode.BadRequest
      case Some(InvalidDIDUrl(_))           => StatusCode.BadRequest
      case Some(NotFound)                   => StatusCode.NotFound
      case Some(RepresentationNotSupported) => StatusCode.NotAcceptable
      case Some(InternalError(_))           => StatusCode.InternalServerError
      case Some(_)                          => StatusCode.InternalServerError
    }
  }
}
