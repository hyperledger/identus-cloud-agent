package org.hyperledger.identus.pollux.core.service.uriResolvers

import io.lemonlabs.uri.DataUrl
import org.hyperledger.identus.shared.http.{GenericUriResolverError, InvalidUri, UriResolver}
import zio.*

class DataUrlResolver extends UriResolver {
  override def resolve(dataUrl: String): IO[GenericUriResolverError, String] = {

    DataUrl.parseOption(dataUrl).fold(ZIO.fail(InvalidUri(dataUrl))) { url =>
      ZIO.succeed(String(url.data, url.mediaType.charset))
    }
  }
}

object DataUrlResolver {
  val layer: ULayer[DataUrlResolver] = ZLayer.succeed(new DataUrlResolver)
}
