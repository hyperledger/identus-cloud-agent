package io.iohk.atala.resolvers

import io.iohk.atala.resolvers.AliceDidDoc.didDocAlice
import io.iohk.atala.resolvers.BobDidDoc.didDocBob
import io.iohk.atala.resolvers.MediatorDidDoc.didDocMediator
import org.didcommx.didcomm.diddoc._

import java.util.Optional
import scala.jdk.CollectionConverters._

object UniversalDidResolver extends DIDDocResolver {

  val diddocs = Map(
    "did:example:alice" -> didDocAlice,
    "did:example:mediator" -> didDocMediator,
    "did:example:bob" -> didDocBob
  ).asJava
  override def resolve(did: String): Optional[DIDDoc] = new DIDDocResolverInMemory(diddocs).resolve(did)
}
