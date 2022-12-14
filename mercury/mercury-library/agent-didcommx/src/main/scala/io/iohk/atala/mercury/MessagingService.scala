package io.iohk.atala.mercury

import scala.jdk.CollectionConverters.*
import zio._

import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.resolvers.DIDResolver
import org.didcommx.didcomm.common.VerificationMethodType
import org.didcommx.didcomm.common.VerificationMaterialFormat

type HttpOrDID = String //TODO
case class ServiceEndpoint(uri: HttpOrDID, accept: Option[Seq[String]], routingKeys: Option[Seq[String]])

object MessagingService {

  /** Encrypt and send a Message via HTTP
    *
    * TODO Move this method to another model
    */
  def send(msg: Message): ZIO[DidComm & DIDResolver & HttpClient, SendMessageError, HttpResponseBody] = { // TODO create error for Throwable
    for {
      didCommService <- ZIO.service[DidComm]
      resolver <- ZIO.service[DIDResolver]
      sendToDID <- msg.to match {
        case Seq() => // TODO support for anonymous message
          ZIO.fail(new RuntimeException("Missing the destination DID - TODO support for anonymous message"))
        case Seq(value) =>
          ZIO.succeed(value)
        case head +: tail => // TODO support for multiple destinations
          ZIO.fail(new RuntimeException("TODO multiple destinations"))
      }

      encryptedForwardMessage <- didCommService.packEncryptedForward(msg, to = msg.to.head) // TODO head
      jsonString = encryptedForwardMessage.string

      didCommService <- resolver
        .didCommServices(sendToDID) /* Seq[DIDCommService] */
        .flatMap {
          case Seq() =>
            ZIO.fail(new RuntimeException("To send a Message you need a destination")) // TODO ERROR
          case Seq(v) =>
            ZIO.succeed(
              ServiceEndpoint(
                uri = v.getServiceEndpoint(),
                accept = Option(v.getAccept()).map(_.asScala.toSeq),
                routingKeys = Option(v.getRoutingKeys()).map(_.asScala.toSeq)
              )
            )
          case headDID +: tail =>
            ZIO.logError("TODO multiple destinations") *>
              ZIO.succeed(
                ServiceEndpoint(
                  uri = headDID.getServiceEndpoint(),
                  accept = Option(headDID.getAccept()).map(_.asScala.toSeq),
                  routingKeys = Option(headDID.getRoutingKeys()).map(_.asScala.toSeq)
                )
              )
        }

      serviceEndpoint = didCommService.uri // TODO this is not safe

      _ <- Console.printLine("Sending to" + serviceEndpoint)

      res <- HttpClient.postDIDComm(serviceEndpoint, jsonString)

    } yield (res)
  }.catchAll { case ex =>
    ZIO.fail(SendMessageError(ex))
  }

// WIP do not delete!
//   /** Encrypt and send a Message via HTTP
//     *
//     * TODO move to another module (agent module)
//     */
//   def sendMessage(msg: Message): ZIO[DidComm & DIDResolver, Throwable, HttpResponseBody] = { // TODO  Throwable
//     for {
//       didCommService <- ZIO.service[DidComm]
//       resolver <- ZIO.service[DIDResolver]
//       sendToDID <- msg.to match {
//         case Seq() => // TODO support for anonymous message
//           ZIO.fail(new RuntimeException("Missing the destination DID - TODO support for anonymous message"))
//         case Seq(value) =>
//           ZIO.succeed(value)
//         case head +: tail => // TODO support for multiple destinations
//           ZIO.fail(new RuntimeException("TODO multiple destinations"))
//       }

//       didCommService <- resolver
//         .didCommServices(sendToDID) /* Seq[DIDCommService] */
//         .flatMap {
//           case Seq() =>
//             ZIO.fail(new RuntimeException("To send a Message you need a destination")) // TODO ERROR
//           case Seq(v) =>
//             ZIO.succeed(
//               ServiceEndpoint(
//                 uri = v.getServiceEndpoint(),
//                 accept = Option(v.getAccept()).map(_.asScala.toSeq),
//                 routingKeys = Option(v.getRoutingKeys()).map(_.asScala.toSeq)
//               )
//             )
//           case headDID +: tail =>
//             ZIO.logError("TODO multiple destinations") *>
//               ZIO.succeed(
//                 ServiceEndpoint(
//                   uri = headDID.getServiceEndpoint(),
//                   accept = Option(headDID.getAccept()).map(_.asScala.toSeq),
//                   routingKeys = Option(headDID.getRoutingKeys()).map(_.asScala.toSeq)
//                 )
//               )
//         }

//       sendToHttp <- didCommService match { // TODO
//         case ServiceEndpoint(uri: HttpOrDID, _, _) if uri.startsWith("http") => ZIO.succeed(uri)
//         case ServiceEndpoint(uri: HttpOrDID, _, _) if uri.startsWith("did")  => ZIO.fail(???) // FIXME
//       }

//       keys <- didCommService match { // TODO
//         case ServiceEndpoint(_, _, None | Some(Seq())) => ZIO.succeed(Seq())
//         case ServiceEndpoint(_, _, Some(routingKeys)) =>
//           ZIO.forall(routingKeys) { key =>
//             val did = DidId(key.split("#", 1)(1))
//             val allKeys = resolver
//               .resolveDID(did)
//               .map(e =>
//                 e.getVerificationMethods()
//                   .asScala
//                   .toSeq
//                   .find(_.getId() == key)
//                   .map { vm =>
//                     vm.getVerificationMaterial().getFormat() match
//                       case VerificationMaterialFormat.JWK =>
//                         vm.getVerificationMaterial().getValue()
//                       case VerificationMaterialFormat.BASE58    => ZIO.fail(???) // FIXME
//                       case VerificationMaterialFormat.MULTIBASE => ZIO.fail(???) // FIXME
//                       case VerificationMaterialFormat.OTHER     => ZIO.fail(???) // FIXME
//                   }
//               )

//             ZIO.succeed(Seq()) // FIXME
//           }

//       }

//       serviceEndpoint = {
//         // didCommService.getRoutingKeys().to
//         ???
//       }

//       encryptedForwardMessage <- didCommService.packEncrypted(msg, to = sendToDID)
//       jsonString = encryptedForwardMessage.string

//       _ <- Console.printLine("Sending to" + serviceEndpoint)

//       res <- postDIDComm(serviceEndpoint)
//     } yield (res)
//   }
}
