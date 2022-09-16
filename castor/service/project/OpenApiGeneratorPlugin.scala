import sbt.Keys.sourceManaged
import sbt.io.syntax._
import sbt.{AutoPlugin, Compile, Def, File, settingKey, taskKey}

object OpenApiGeneratorPlugin extends AutoPlugin {

  object autoImport {
    val openApiGeneratorSpec = settingKey[File]("The OpenAPI specification file.")
    val openApiGeneratorConfig = settingKey[File]("The generator config file.")
    val openApiGenerateClasses = taskKey[Seq[File]]("Generate API & model classes.")
  }

  import autoImport._
  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    openApiGenerateClasses := {
      import org.openapitools.codegen.DefaultGenerator
      import org.openapitools.codegen.config.CodegenConfigurator

      import scala.collection.JavaConverters._
      val configurator = CodegenConfigurator.fromFile(openApiGeneratorConfig.value.getPath)
      configurator.setInputSpec(openApiGeneratorSpec.value.getPath)
      configurator.setOutputDir(((Compile / sourceManaged).value / "openapi").getPath)
      configurator.setValidateSpec(true)
      val gen = new DefaultGenerator()
      gen.opts(configurator.toClientOptInput)
      gen.generate().asScala
    }
  )
}
