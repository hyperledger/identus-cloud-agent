package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.did.w3c.DIDResolutionErrorRepr
import io.iohk.atala.castor.core.model.error.DIDResolutionError
import io.iohk.atala.castor.core.service.DIDService
import zio.*

package object w3c {

  extension (didData: DIDData) {
    def repr: DIDDocumentRepr = ???
  }

  def makeW3CResolver(service: DIDService)(did: String): UIO[DIDResolutionRepr] = {
    val didData = for {
      prismDID <- ZIO
        .fromEither(PrismDID.fromString(did))
        .mapError(_ => DIDResolutionErrorRepr.InvalidDID)
        .map(_.asCanonical)
      didData <- service
        .resolveDID(prismDID)
        .mapBoth(
          {
            case DIDResolutionError.DLTProxyError(_)       => DIDResolutionErrorRepr.InternalError
            case DIDResolutionError.UnexpectedDLTResult(_) => DIDResolutionErrorRepr.InternalError
          },
          _.toRight(DIDResolutionErrorRepr.NotFound)
        )
        .absolve
    } yield didData.repr

    didData
      .foldZIO(
        error => ZIO.succeed(DIDResolutionRepr(didResolutionMetadata = DIDResolutionMetadataRepr(error = Some(error)))),
        didDocument =>
          ZIO.succeed(
            DIDResolutionRepr(
              didDocument = Some(didDocument),
              didDocumentMetadata = Some(DIDDocumentMetadataRepr(deactivated = Some(false))) // TODO: handle deactivated DIDs
            )
          )
      )
  }

}
