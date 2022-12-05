package io.iohk.atala.agent.server.jobs

import scala.jdk.CollectionConverters.*

import zio._
import io.iohk.atala.mercury._
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.resolvers.UniversalDidResolver
import zhttp.http._
import zhttp.service._

object MercuryUtils {

  val env = ChannelFactory.auto ++ EventLoopGroup.auto()

  /** Encrypt and send a Message via HTTP
    *
    * TODO Move this method to another model
    */
  def sendMessage(msg: Message): ZIO[DidComm, MercuryException, Unit] = { // TODO  Throwable
    for {
      didCommService <- ZIO.service[DidComm]

      encryptedForwardMessage <- didCommService.packEncrypted(msg, to = msg.to.get)
      jsonString = encryptedForwardMessage.string

      serviceEndpoint = UniversalDidResolver
        .resolve(msg.to.get.value) // TODO GET
        .get()
        .getDidCommServices()
        .asScala
        .toSeq
        .headOption
        .map(s => s.getServiceEndpoint())
        .get // TODO make ERROR type

      _ <- Console.printLine("Sending to" + serviceEndpoint)

      res <- Client
        .request(
          url = serviceEndpoint,
          method = Method.POST,
          headers = Headers("content-type" -> MediaTypes.contentTypeEncrypted),
          content = Body.fromChunk(Chunk.fromArray(jsonString.getBytes))
          // ssl = ClientSSLOptions.DefaultSSL,
        )
        .provideSomeLayer(env)
        .catchNonFatalOrDie { ex => ZIO.fail(SendMessage(ex)) }
      data <-
        if (res.status.isSuccess)
          res.body.asString
            .catchNonFatalOrDie { ex => ZIO.fail(ParseResponse(ex)) }
        else
          ZIO.fail(
            ParseResponse(RuntimeException(s"Received non-success HTTP response status from peer agent: ${res.status}"))
          )
      _ <- Console.printLine(data)
    } yield ()
  }
}
