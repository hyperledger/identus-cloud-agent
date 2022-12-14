package io.iohk.atala.resolvers

import io.iohk.atala.resolvers.AliceDidDoc.didDocAlice
import io.iohk.atala.resolvers.BobDidDoc.didDocBob
import io.iohk.atala.resolvers.MediatorDidDoc.didDocMediator
import org.didcommx.didcomm.diddoc._

import java.util.Optional
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

import zio._
import io.iohk.atala.mercury.model.DidId

trait DIDResolver {
  def resolveDID(did: DidId): Task[DIDDoc] // TODO return Task[DIDDocument]

  def didCommServices(did: DidId): Task[Seq[DIDCommService]] =
    resolveDID(did).map(_.getDidCommServices().asScala.toSeq)
}
object DIDResolver {
  val layer = ZLayer.succeed(UniversalDidResolver)
}

object UniversalDidResolver extends DIDDocResolver with DIDResolver {

  override def resolveDID(did: DidId): Task[DIDDoc] =
    ZIO.attempt(resolve(did.value).toScala).flatMap {
      case None        => ZIO.fail(new java.lang.RuntimeException("resolve fail"))
      case Some(value) => ZIO.succeed(value)
    }

  val diddocs = Map(
    "did:example:alice" -> didDocAlice,
    "did:example:mediator" -> didDocMediator,
    "did:example:bob" -> didDocBob
  ).asJava

  override def resolve(did: String): Optional[DIDDoc] = {
    val regex = "(did:peer:.+)".r
    did match {
      case regex(peer) =>
        // val peerDidResolver = PeerDidResolverImpl()
        // val didDocJson = peerDidResolver.resolveDidAsJson(peer)
        Some(PeerDidResolver.getDIDDoc(peer)).toJava
      case anydid: String => new DIDDocResolverInMemory(diddocs).resolve(anydid)
    }
  }

}
