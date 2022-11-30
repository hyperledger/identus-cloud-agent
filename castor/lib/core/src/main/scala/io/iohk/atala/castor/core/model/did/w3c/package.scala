package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.did.w3c.DIDResolutionErrorRepr
import io.iohk.atala.castor.core.model.error.DIDResolutionError
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*

package object w3c {

  import W3CModelHelper.*

  /** A wrapper for DID resolution in W3C format */
  def makeW3CResolver(
      service: DIDService
  )(did: String): IO[DIDResolutionErrorRepr, (DIDDocumentMetadataRepr, DIDDocumentRepr)] = {
    for {
      prismDID <- ZIO
        .fromEither(PrismDID.fromString(did))
        .mapError(_ => DIDResolutionErrorRepr.InvalidDID)
      didData <- service
        .resolveDID(prismDID)
        .tap(i => Console.printLine(i.map(_._1.lastOperationHash.toArray).map(HexString.fromByteArray)))
        .mapBoth(
          {
            case DIDResolutionError.DLTProxyError(_)       => DIDResolutionErrorRepr.InternalError
            case DIDResolutionError.UnexpectedDLTResult(_) => DIDResolutionErrorRepr.InternalError
          },
          _.toRight(DIDResolutionErrorRepr.NotFound)
        )
        .absolve
    } yield (didData._1.toW3C, didData._2.toW3C)
  }

}
