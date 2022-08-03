package io.iohk.atala

import cats.syntax.all._
import io.iohk.atala.resolvers.{AliceSecretResolver, BobSecretResolver, UniversalDidResolver}
import org.didcommx.didcomm.DIDComm
import org.didcommx.didcomm.message.MessageBuilder
import org.didcommx.didcomm.model.PackEncryptedParams.Builder
import org.didcommx.didcomm.model.UnpackParams
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.interop.catz._
import zio.{Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}
import scala.io.StdIn
import scala.jdk.CollectionConverters._

object Main extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val didComm = new DIDComm(UniversalDidResolver, AliceSecretResolver.secretResolver)

    val id = "1234567890"
    val body = Map("messagespecificattribute" -> "and its value").asJava
    val `type` = "http://example.com/protocols/lets_do_lunch/1.0/proposal"
    val message = new MessageBuilder(id, body, `type`)
    val ALICE_DID = "did:example:alice"
    val BOB_DID = "did:example:bob"
    message.from(ALICE_DID)
    message.to(Seq(BOB_DID).asJava)
    message.createdTime(1516269022)
    message.expiresTime(1516385931)
    val xxx = message.build()

    val xx = new Builder(xxx, BOB_DID)
    val packResult = didComm.packEncrypted(
      xx
        .from(ALICE_DID)
        .build()
    )
    println(s"**************************************************************************************************************************")
    println(s"Sending ${packResult.getPackedMessage} to ${Option(packResult.getServiceMetadata).map(_.getServiceEndpoint)}")
    println(s"**************************************************************************************************************************")

    val unpackResult = didComm.unpack(
      new UnpackParams.Builder(packResult.getPackedMessage)
        .secretResolver(BobSecretResolver.secretResolver)
        .build()
    )

    println(s"**************************************************************************************************************************")
    println(s"\nGot ${unpackResult.getMessage} message\n")
    println(s"**************************************************************************************************************************")

    // println(s" ${packResult.getServiceMetadata}")
    val routes =
      ZHttp4sServerInterpreter().from(Endpoints.all).toRoutes <+> new SwaggerHttp4s(Endpoints.yaml).routes

    BlazeServerBuilder[Task]
      .withExecutionContext(runtime.executor.asExecutionContext)
      .bindHttp(8080, "localhost")
      .withHttpApp(Router("/" -> routes).orNotFound)
      .resource
      .use { _ =>
        ZIO.succeedBlocking {
          println("Server started at http://localhost:8080. \n Open API docs at http://localhost:8080/docs. \n Press ENTER key to exit.")
          StdIn.readLine()
        }
      }

  }
}
