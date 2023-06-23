package io.iohk.atala.agent.server

import zio.*
import zio.http.ServerConfig
import zio.http.Server
import io.iohk.atala.pollux.credentialschema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.credentialschema.VerificationPolicyServerEndpoints
import io.iohk.atala.connect.controller.ConnectionServerEndpoints
import io.iohk.atala.issue.controller.IssueServerEndpoints
import io.iohk.atala.castor.controller.DIDServerEndpoints
import io.iohk.atala.castor.controller.DIDRegistrarServerEndpoints
import io.iohk.atala.presentproof.controller.PresentProofServerEndpoints
import io.iohk.atala.system.controller.SystemServerEndpoints
import io.iohk.atala.agent.server.http.ZHttpEndpoints
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.http.ZHttp4sBlazeServer

/** Contains things that will be run by an application
  */
object PrismAgentApp {

  def mainApp(didCommServicePort: Int) = for {
    _ <- Modules.issueCredentialDidCommExchangesJob.debug.fork
    _ <- Modules.presentProofExchangeJob.debug.fork
    _ <- Modules.connectDidCommExchangesJob.debug.fork
    _ <- Modules.syncDIDPublicationStateFromDltJob.fork
    _ <- AgentHttpServer.run.fork
    fiber <- DidCommHttpServer.run(didCommServicePort).fork
    _ <- fiber.join *> ZIO.log(s"Server End")
    _ <- ZIO.never
  } yield ()

}

object AgentHttpServer {
  def run =
    for {
      allSchemaRegistryEndpoints <- SchemaRegistryServerEndpoints.all
      allVerificationPolicyEndpoints <- VerificationPolicyServerEndpoints.all
      allConnectionEndpoints <- ConnectionServerEndpoints.all
      allIssueEndpoints <- IssueServerEndpoints.all
      allDIDEndpoints <- DIDServerEndpoints.all
      allDIDRegistrarEndpoints <- DIDRegistrarServerEndpoints.all
      allPresentProofEndpoints <- PresentProofServerEndpoints.all
      allSystemEndpoints <- SystemServerEndpoints.all
      allEndpoints = ZHttpEndpoints.withDocumentations[Task](
        allSchemaRegistryEndpoints ++
          allVerificationPolicyEndpoints ++
          allConnectionEndpoints ++
          allDIDEndpoints ++
          allDIDRegistrarEndpoints ++
          allIssueEndpoints ++
          allPresentProofEndpoints ++
          allSystemEndpoints
      )
      appConfig <- ZIO.service[AppConfig]
      httpServer <- ZHttp4sBlazeServer.start(allEndpoints, port = appConfig.agent.httpEndpoint.http.port)
    } yield ()
}

object DidCommHttpServer {
  def run(didCommServicePort: Int) = {
    val server = {
      val config = ServerConfig(address = new java.net.InetSocketAddress(didCommServicePort))
      ServerConfig.live(config)(using Trace.empty) >>> Server.live
    }
    for {
      _ <- ZIO.logInfo(s"Server Started on port $didCommServicePort")
      _ <- Server
        .serve(Modules.didCommServiceEndpoint)
        .provideSomeLayer(server)
        .debug *> ZIO.logWarning(s"Server STOP (on port $didCommServicePort)")
    } yield ()
  }
}
