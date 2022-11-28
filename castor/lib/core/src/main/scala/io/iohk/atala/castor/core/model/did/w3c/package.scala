package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.did.w3c.DIDResolutionErrorRepr
import io.iohk.atala.castor.core.model.error.DIDResolutionError
import io.iohk.atala.castor.core.service.DIDService
import zio.*

package object w3c {

  import W3CModelHelper.*

  def makeW3CResolver(
      service: DIDService
  )(did: String): IO[DIDResolutionErrorRepr, (DIDDocumentRepr, DIDDocumentMetadataRepr)] = {
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
    } yield didData.toW3C

    didData
      .map(didDocument =>
        didDocument -> DIDDocumentMetadataRepr(deactivated = Some(false)) // TODO: handle did deactivation
      )
  }

}
