package io.iohk.atala.resolvers

import org.didcommx.didcomm.diddoc.{DIDCommService, DIDDoc, VerificationMethod}
import org.didcommx.peerdid.PeerDIDResolver.resolvePeerDID
import org.didcommx.peerdid.VerificationMaterialFormatPeerDID
import io.circe.{HCursor, Json}
import io.circe.parser.*
import zio.*
import org.didcommx.didcomm.common._
import scala.jdk.CollectionConverters.*
import io.circe.Decoder

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
  def resolveUnsafe(didPeer: String) =
    parse(resolvePeerDID(didPeer, VerificationMaterialFormatPeerDID.JWK)).toOption.get

  def getDIDDoc(didPeer: String): DIDDoc = {
    val json = resolveUnsafe(didPeer)
    val cursor: HCursor = json.hcursor
    val did = cursor.downField("id").as[String].getOrElse(???)
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
      .getOrElse(List.empty)

    val authentications1 = cursor.downField("authentication").as[List[Json]]
    val verificationMethodList1: List[VerificationMethod] = authentications1
      .map {
        _.map(item =>
          val id = item.hcursor.downField("id").as[String].getOrElse(???)

          val publicKeyJwk = item.hcursor
            .downField("publicKeyJwk")
            .as[Json]
            .map(_.toString)
            .getOrElse(???)

          val controller = item.hcursor.downField("controller").as[String].getOrElse(???)
          val verificationMaterial = new VerificationMaterial(VerificationMaterialFormat.JWK, publicKeyJwk)
          new VerificationMethod(id, VerificationMethodType.JSON_WEB_KEY_2020, verificationMaterial, controller)
        )
      }
      .getOrElse(???)

    val keyIdAuthentications: List[String] = authentications1
      .map {
        _.map(item => item.hcursor.downField("id").as[String].getOrElse(???))
      }
      .getOrElse(???)

    val keyAgreements1 = cursor.downField("keyAgreement").as[List[Json]]
    val verificationMethodList: List[VerificationMethod] = keyAgreements1
      .map {
        _.map(item =>
          val id = item.hcursor.downField("id").as[String].getOrElse(???)

          val publicKeyJwk = item.hcursor
            .downField("publicKeyJwk")
            .as[Json]
            .map(_.toString)
            .getOrElse(???)

          val controller = item.hcursor.downField("controller").as[String].getOrElse(???)
          val verificationMaterial = new VerificationMaterial(VerificationMaterialFormat.JWK, publicKeyJwk)
          new VerificationMethod(id, VerificationMethodType.JSON_WEB_KEY_2020, verificationMaterial, controller)
        )
      }
      .getOrElse(???)

    val keyIds: List[String] = keyAgreements1
      .map {
        _.map(item => item.hcursor.downField("id").as[String].getOrElse(???))
      }
      .getOrElse(???)
    val mergedList = verificationMethodList ++ verificationMethodList1

    val didDoc =
      new DIDDoc(
        did,
        keyIds.asJava,
        keyIdAuthentications.asJava,
        mergedList.asJava,
        didCommServices.asJava
      )
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
