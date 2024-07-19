package org.hyperledger.identus.pollux.core.service.uriResolvers

import org.hyperledger.identus.pollux.vc.jwt.DidResolver
import org.hyperledger.identus.shared.http.UriResolver
import zio.*
import zio.test.*
import zio.test.Assertion.*

object DidUrlResolverSpec extends ZIOSpecDefault {

  class MockHttpUrlResolver extends HttpUrlResolver(null) {
    // TODO: return a schema
    override def resolve(uri: String) = ZIO.succeed("resolved")
  }

  val didResolverLayer = ZLayer.succeed(new DidResolver {
    // TODO: return a normal regular DID document
    override def resolve(didUrl: String) = ???
  })
  val httpUrlResolver = ZLayer.succeed(new MockHttpUrlResolver)

  override def spec = {
    suite("DidUrlResolverSpec")(
      test("test") {
        for {
          didResolver <- ZIO.service[DidResolver]
          httpUrlResolver <- ZIO.service[HttpUrlResolver]
          didUrlResolver = new DidUrlResolver(httpUrlResolver, didResolver)
        } yield {
          assertTrue(true)
        }
      }
    ).provide(didResolverLayer, httpUrlResolver)
  }
}
