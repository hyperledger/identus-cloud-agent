package io.iohk.atala.mercury

import scala.jdk.CollectionConverters.*
import zio._

import io.circe._
import io.circe.Json._
import io.circe.parser._
import io.circe.JsonObject
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.mercury.protocol.routing._
import io.iohk.atala.resolvers.DIDResolver
import org.didcommx.didcomm.common.VerificationMethodType
import org.didcommx.didcomm.common.VerificationMaterialFormat

type HttpOrDID = String //TODO
case class ServiceEndpoint(uri: HttpOrDID, accept: Option[Seq[String]], routingKeys: Option[Seq[String]])

object MessagingService {

  def isForwardMessage[Service <: DidComm, Resolver <: DIDResolver](
    didCommService:  Service,
    resolver: Resolver,
    didCommServiceEndpoint: ServiceEndpoint,
    message: Message, 
    encrypted: EncryptedMessage): ZIO[Any, Throwable, EncryptedMessage] = {
  if (didCommServiceEndpoint.uri.startsWith("did:")) {
    Console.printLine("RoutingDID:" + DidId(didCommServiceEndpoint.uri))
    didCommService.packEncrypted(
        ForwardMessage(
          from = message.from.get,
          to = DidId(didCommServiceEndpoint.uri),
          expires_time = None,
          body = ForwardBody(next = message.to.head), // TODO check msg header
          attachments = Seq(AttachmentDescriptor.buildJsonAttachment(payload = encrypted.asJson)),
        ).asMessage,
        to = DidId(didCommServiceEndpoint.uri)
      )
  } else {
    ZIO.succeed(encrypted)
  }
}

  /** Encrypt and send a Message via HTTP
    *
    * TODO Move this method to another model
    */
  def send(msg: Message): ZIO[DidComm & DIDResolver & HttpClient, SendMessageError, HttpResponseBody] = {
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
      _ <- Console.printLine("Encrypted Message")
      encryptedMessage <- didCommService.packEncrypted(msg, to = msg.to.head) // TODO head

      didCommServiceUrl <- resolver
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
       _ <- Console.printLine("Forward message")
      sendMsg = isForwardMessage(didCommService, resolver, didCommServiceUrl, msg, encryptedMessage)
      jsonString <- sendMsg.map(_.string)

      serviceEndpoint <- if (didCommServiceUrl.uri.startsWith("did:"))
           resolver
             .didCommServices(DidId(didCommServiceUrl.uri))
             .map(_.toSeq.head.getServiceEndpoint()) // TODO this is not safe and also need to be recursive
         else ZIO.succeed(didCommServiceUrl.uri)

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
