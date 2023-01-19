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
case class MessageAndAddress(msg: Message, url: String)

object MessagingService {

  /** Encrypted payload (message) and make the Forward Message */
  def makeForwardMessage(message: Message, mediator: DidId): URIO[DidOps & DidAgent, ForwardMessage] =
    for {
      didCommService <- ZIO.service[DidOps]
      encrypted <- didCommService.packEncrypted(message, to = message.to.head) // TODO head
      msg = ForwardMessage(
        to = mediator,
        expires_time = None,
        body = ForwardBody(next = message.to.head), // TODO check msg head
        attachments = Seq(AttachmentDescriptor.buildJsonAttachment(payload = encrypted.asJson)),
      )
    } yield (msg)

  /** Create a Message and any Forward Message as needed */
  def makeMessage(msg: Message): ZIO[
    DidOps & DidAgent & DIDResolver & HttpClient,
    SendMessageError,
    MessageAndAddress
  ] =
    for {
      didCommService <- ZIO.service[DidOps]
      resolver <- ZIO.service[DIDResolver]
      sendToDID <- msg.to match {
        case Seq() => // TODO support for anonymous message
          ZIO.fail(
            SendMessageError(
              new RuntimeException("Missing the destination DID - TODO support for anonymous message")
            )
          )
        case Seq(value) =>
          ZIO.succeed(value)
        case head +: tail => // TODO support for multiple destinations
          ZIO.fail(
            SendMessageError(
              new RuntimeException("TODO multiple destinations")
            )
          )
      }

      serviceEndpoint <- resolver
        .didCommServices(sendToDID) /* Seq[DIDCommService] */
        .catchAll { case ex => ZIO.fail(SendMessageError(ex)) }
        .flatMap {
          case Seq() =>
            ZIO.fail(
              SendMessageError(
                new RuntimeException("To send a Message you need a destination") // TODO ERROR
              )
            )
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

      msgToSend <- serviceEndpoint match {
        case ServiceEndpoint(url, _, None) if url.startsWith("http") =>
          ZIO.log(s"No Forward Message needed. (send to $url)") *>
            ZIO.succeed(MessageAndAddress(msg, url))
        case ServiceEndpoint(url, _, Some(Seq())) if url.startsWith("http") =>
          ZIO.log(s"No Forward Message needed. (send to $url)") *>
            ZIO.succeed(MessageAndAddress(msg, url))
        case ServiceEndpoint(did, _, _) if did.startsWith("did:") =>
          for {
            _ <- ZIO.log(s"Make Forward Message for Mediator '$did'")
            mediator = DidId(did)
            forwardMessage <- makeForwardMessage(message = msg, mediator = mediator)
            finalMessage <- makeMessage(forwardMessage.asMessage) // Maybe it needs a double warping
          } yield finalMessage
        case ServiceEndpoint(uri, _, Some(routingKeys)) =>
          ZIO.log(s"RoutingDID: $routingKeys") *> ??? // ZIO.fail(???) // FIXME no support for routingKeys
        case s @ ServiceEndpoint(_, _, None) =>
          ZIO.logError(s"Unxpected ServiceEndpoint $s") *> ZIO.fail(
            SendMessageError(new RuntimeException(s"Unxpected ServiceEndpoint $s"))
          )
      }
    } yield (msgToSend)

    /** Encrypt and send a Message via HTTP
      *
      * TODO Move this method to another model
      */
  def send(msg: Message): ZIO[DidOps & DidAgent & DIDResolver & HttpClient, SendMessageError, HttpResponseBody] =
    for {
      auxFinalMessage <- makeMessage(msg)
      MessageAndAddress(finalMessage, serviceEndpoint) = auxFinalMessage
      didCommService <- ZIO.service[DidOps]
      encryptedMessage <-
        if (finalMessage.`type` == ForwardMessage.PIURI)
          didCommService.packEncryptedAnon(msg = finalMessage, to = finalMessage.to.head) // TODO Head
        else
          didCommService.packEncrypted(msg = finalMessage, to = finalMessage.to.head) // TODO Head

      _ <- ZIO.log(s"Sending to Message to '$serviceEndpoint'")
      res <- HttpClient
        .postDIDComm(url = serviceEndpoint, data = encryptedMessage.string)
        .catchAll { case ex => ZIO.fail(SendMessageError(ex, Some(encryptedMessage.string))) }
    } yield (res)

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
