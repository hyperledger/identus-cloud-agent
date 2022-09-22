package io.iohk.atala.resolvers

import io.iohk.atala.resolvers.UniversalDidResolver.diddocs
import org.didcommx.didcomm.diddoc.{DIDDoc, DIDDocResolver, DIDDocResolverInMemory}
import org.didcommx.peerdid.PeerDIDResolver.resolvePeerDID
import org.didcommx.peerdid.VerificationMaterialFormatPeerDID
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import zio._
import zio.{Console, Task, UIO, URLayer, ZIO}

trait PeerDidResolver {
  def resolve(did: String): UIO[String]
  def resolveDidAsJson(did: String): UIO[Option[Json]]
}

case class PeerDidResolverImpl() extends PeerDidResolver {

  def resolve(did: String): UIO[String] = {
    ZIO.succeed { resolvePeerDID(did, VerificationMaterialFormatPeerDID.MULTIBASE) }
  }

  def resolveDidAsJson(did: String): UIO[Option[Json]] = {
    ZIO.succeed {
      parse(resolvePeerDID(did, VerificationMaterialFormatPeerDID.MULTIBASE)).toOption
    }
  }
}

object PeerDidResolver {

  val layer: ULayer[PeerDidResolver] = {
    ZLayer.succeedEnvironment(
      ZEnvironment(PeerDidResolverImpl())
    )
  }

  def resolve(did: String): URIO[PeerDidResolver, String] = {
    ZIO.serviceWithZIO(_.resolve(did))
  }

  def resolveDidAsJson(did: String): URIO[PeerDidResolver, Option[Json]] = {
    ZIO.serviceWithZIO(_.resolveDidAsJson(did))
  }
}
