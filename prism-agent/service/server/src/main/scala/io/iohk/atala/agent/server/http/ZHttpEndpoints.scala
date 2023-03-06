package io.iohk.atala.agent.server.http

import zio.{Task, ZIO, ZLayer, URIO}
import io.iohk.atala.pollux.credentialschema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaController
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
//import sttp.tapir.ztapir.{RichZServerEndpoint, ZServerEndpoint}
import sttp.tapir.redoc.RedocUIOptions
import sttp.tapir.server.ServerEndpoint
import scala.concurrent.Future

object ZHttpEndpoints {
  def swaggerEndpoints[F[_]](apiEndpoints: List[ServerEndpoint[Any, F]]): List[ServerEndpoint[Any, F]] =
    SwaggerInterpreter().fromServerEndpoints[F](apiEndpoints, "Prism Agent", "1.0.0")

  def redocEndpoints[F[_]](apiEndpoints: List[ServerEndpoint[Any, F]]): List[ServerEndpoint[Any, F]] =
    RedocInterpreter(redocUIOptions = RedocUIOptions.default.copy(pathPrefix = List("redoc")))
      .fromServerEndpoints[F](apiEndpoints, title = "Prism Agent", version = "1.0.0")

  def withDocumentations[F[_]](apiEndpoints: List[ServerEndpoint[Any, F]]): List[ServerEndpoint[Any, F]] = {
    apiEndpoints ++ swaggerEndpoints[F](apiEndpoints) ++ redocEndpoints[F](apiEndpoints)
  }
}
