package io.iohk.atala.castor.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.castor.controller.http.DIDResolutionResult
import sttp.model.StatusCode
import zio.*
import io.iohk.atala.castor.controller.http.{DIDDocument, DIDDocumentMetadata, DIDResolutionMetadata}
import io.iohk.atala.castor.core.model.did.w3c.{
  PublicKeyReprOrRef,
  DIDDocumentMetadataRepr,
  DIDDocumentRepr,
  DIDResolutionErrorRepr
}

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
