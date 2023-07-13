package io.iohk.atala.api.util

import io.iohk.atala.castor.controller.{
  DIDController,
  DIDRegistrarController,
  DIDRegistrarServerEndpoints,
  DIDServerEndpoints
}
import io.iohk.atala.connect.controller.{ConnectionController, ConnectionServerEndpoints}
import io.iohk.atala.issue.controller.{IssueController, IssueServerEndpoints}
import io.iohk.atala.pollux.credentialschema.controller.{CredentialSchemaController, VerificationPolicyController}
import io.iohk.atala.pollux.credentialschema.{SchemaRegistryServerEndpoints, VerificationPolicyServerEndpoints}
import io.iohk.atala.presentproof.controller.{PresentProofController, PresentProofServerEndpoints}
import io.iohk.atala.system.controller.{SystemController, SystemServerEndpoints}
import org.scalatestplus.mockito.MockitoSugar.*
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.Using

object Tapir2StaticOAS extends ZIOAppDefault {

  @main override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val effect = for {
      args <- getArgs
      allSchemaRegistryEndpoints <- SchemaRegistryServerEndpoints.all
      allVerificationPolicyEndpoints <- VerificationPolicyServerEndpoints.all
      allConnectionEndpoints <- ConnectionServerEndpoints.all
      allIssueEndpoints <- IssueServerEndpoints.all
      allDIDEndpoints <- DIDServerEndpoints.all
      allDIDRegistrarEndpoints <- DIDRegistrarServerEndpoints.all
      allPresentProofEndpoints <- PresentProofServerEndpoints.all
      allSystemEndpoints <- SystemServerEndpoints.all
      allEndpoints = allSchemaRegistryEndpoints ++
        allVerificationPolicyEndpoints ++
        allConnectionEndpoints ++
        allDIDEndpoints ++
        allDIDRegistrarEndpoints ++
        allIssueEndpoints ++
        allPresentProofEndpoints ++
        allSystemEndpoints
    } yield {
      import sttp.apispec.openapi.circe.yaml.*
      val yaml = OpenAPIDocsInterpreter().toOpenAPI(allEndpoints.map(_.endpoint), "Prism Agent", "1.0.0").toYaml
      val path = Path.of(args.headOption.getOrElse("prism-agent-oas.yaml"))
      Using(Files.newBufferedWriter(path, StandardCharsets.UTF_8)) { writer => writer.write(yaml) }
    }
    effect.provideSomeLayer(
      ZLayer.succeed(mock[ConnectionController]) ++
        ZLayer.succeed(mock[CredentialSchemaController]) ++
        ZLayer.succeed(mock[VerificationPolicyController]) ++
        ZLayer.succeed(mock[DIDRegistrarController]) ++
        ZLayer.succeed(mock[PresentProofController]) ++
        ZLayer.succeed(mock[IssueController]) ++
        ZLayer.succeed(mock[DIDController]) ++
        ZLayer.succeed(mock[SystemController])
    )
  }

}
