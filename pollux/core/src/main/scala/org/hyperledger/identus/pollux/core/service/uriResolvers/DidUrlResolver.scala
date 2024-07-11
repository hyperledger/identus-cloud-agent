package org.hyperledger.identus.pollux.core.service.uriResolvers

import org.hyperledger.identus.pollux.vc.jwt.DidResolver
import org.hyperledger.identus.shared.http.{GenericUriResolverError, UriResolver}
import zio.*

class DidUrlResolver(httpUrlResolver: HttpUrlResolver, didResolver: DidResolver) extends UriResolver {
  def resolve(uri: String): IO[GenericUriResolverError, String] = {
    ??? // TODO: implement did URL resolver
  }

}

object DidUrlResolver {
  val layer: URLayer[HttpUrlResolver & DidResolver, DidUrlResolver] =
    ZLayer.fromFunction(DidUrlResolver(_, _))
}
