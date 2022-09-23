package io.iohk.atala.resolvers

import io.circe.Decoder.Result
import io.iohk.atala.resolvers.UniversalDidResolver.diddocs
import org.didcommx.didcomm.diddoc.{DIDCommService, DIDDoc, DIDDocResolver, DIDDocResolverInMemory, VerificationMethod}
import org.didcommx.peerdid.PeerDIDResolver.resolvePeerDID
import org.didcommx.peerdid.VerificationMaterialFormatPeerDID
import io.circe.{HCursor, Json}
import io.circe.generic.auto.*
import io.circe.parser.*
import io.iohk.atala.resolvers.AliceDidDoc.verficationMethods
import zio.*
import zio.{Console, Task, UIO, URLayer, ZIO}

import scala.jdk.CollectionConverters.*
import java.util.Optional

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
  import io.circe.Decoder, io.circe.generic.auto._
  def resolveUnsafe(didPeer: String) =
    parse(resolvePeerDID(didPeer, VerificationMaterialFormatPeerDID.MULTIBASE)).toOption.get

  def getDIDDoc(didPeer: String): DIDDoc = {
    val json = resolveUnsafe(didPeer)
    val cursor: HCursor = json.hcursor
    val did = cursor.downField("id").as[String].getOrElse(???)
    val authentications = cursor
      .downField("authentication")
      .as[List[Json]]
      .getOrElse(???)
      .map(_.toString)
    val keyAgreements: Seq[String] = cursor.downField("keyAgreement").as[List[Json]].getOrElse(???).map(_.toString)
    val service = cursor.downField("service").as[List[Json]]

    val didCommServices: List[DIDCommService] = service
      .map {
        _.map { item =>
          val id = item.hcursor.downField("id").as[String].getOrElse(???)
          // val typ = item.hcursor.downField("type").as[String].getOrElse(???)
          val serviceEndpoint = item.hcursor.downField("serviceEndpoint").as[String].getOrElse(???)
          val routingKeys: Seq[String] = item.hcursor.downField("routingKeys").as[List[String]].getOrElse(Seq.empty)
          val accept: Seq[String] = item.hcursor.downField("accept").as[List[String]].getOrElse(Seq.empty)
          new DIDCommService(id, serviceEndpoint, routingKeys.asJava, accept.asJava)
        }
      }
      .getOrElse(???)
    val verficationMethods = Seq.empty[VerificationMethod]

    val didDoc =
      new DIDDoc(did, keyAgreements.asJava, authentications.asJava, verficationMethods.asJava, didCommServices.asJava)
    didDoc
  }

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

// object PeerDid {

//   val keyAgreement = VerificationMaterialPeerDID[VerificationMethodTypeAgreement](
//     VerificationMaterialFormatPeerDID.MULTIBASE,
//     "z6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc",
//     VerificationMethodTypeAgreement.X25519_KEY_AGREEMENT_KEY_2020.INSTANCE
//   )
//   val keyAuthentication = VerificationMaterialPeerDID[VerificationMethodTypeAuthentication](
//     VerificationMaterialFormatPeerDID.MULTIBASE,
//     "z6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V",
//     VerificationMethodTypeAuthentication.ED25519_VERIFICATION_KEY_2020.INSTANCE
//   )

//   val service =
//     """[{
//       |  "type": "DIDCommMessaging",
//       |  "serviceEndpoint": "http://localhost:8000/",
//       |  "routingKeys": ["did:example:somemediator#somekey"]
//       |},
//       |{
//       |  "type": "example",
//       |  "serviceEndpoint": "http://localhost:8000/",
//       |  "routingKeys": ["did:example:somemediator#somekey2"],
//       |  "accept": ["didcomm/v2", "didcomm/aip2;env=rfc587"]
//       |}]""".stripMargin

//   def example = org.didcommx.peerdid.PeerDIDCreator.createPeerDIDNumalgo2(
//     List(keyAgreement).asJava,
//     List(keyAuthentication).asJava,
//     service
//   )
// }
