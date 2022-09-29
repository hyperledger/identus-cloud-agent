package io.iohk.atala.resolvers

import io.iohk.atala.resolvers.AliceDidDoc.didDocAlice
import io.iohk.atala.resolvers.BobDidDoc.didDocBob
import io.iohk.atala.resolvers.MediatorDidDoc.didDocMediator
import org.didcommx.didcomm.diddoc._

import java.util.Optional
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

object UniversalDidResolver extends DIDDocResolver {

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
        println(PeerDidResolver.getDIDDoc(peer))
        println("----------------")
        Some(PeerDidResolver.getDIDDoc(peer)).toJava
      case anydid: String => new DIDDocResolverInMemory(diddocs).resolve(anydid)
    }
  }

}
