package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.error.DIDResolutionError
import io.iohk.atala.castor.core.service.DIDService
import zio.*

package object w3c {

  import W3CModelHelper.*

  /** A wrapper for DID resolution in W3C format */
  def makeW3CResolver(
      service: DIDService
  )(did: String): IO[DIDResolutionErrorRepr, (DIDDocumentMetadataRepr, DIDDocumentRepr)] = {
    import DIDResolutionError.*
    for {
      prismDID <- ZIO
        .fromEither(PrismDID.fromString(did))
        .mapError(e => DIDResolutionErrorRepr.InvalidDID(e))
      didData <- service
        .resolveDID(prismDID)
        .tapError {
          case ValidationError(_) => ZIO.unit
          case error              => ZIO.logError(error.toString)
        }
        .mapBoth(
          {
            case DLTProxyError(_) =>
              DIDResolutionErrorRepr.InternalError("Error occurred while connecting to Prism Node")
            case UnexpectedDLTResult(_) =>
              DIDResolutionErrorRepr.InternalError("Unexpected result obtained from Prism Node")
            case ValidationError(e) => DIDResolutionErrorRepr.InvalidDID(e.toString)
          },
          _.toRight(DIDResolutionErrorRepr.NotFound)
        )
        .absolve
    } yield {
      // https://www.w3.org/TR/did-core/#dfn-diddocument
      // The value of id in the resolved DID document MUST match the DID that was resolved.
      (didData._1.toW3C(prismDID), didData._2.toW3C(prismDID))
    }
  }

}
