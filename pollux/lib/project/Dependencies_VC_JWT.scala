import sbt._

//TODO REMOVE THIS
object Dependencies_VC_JWT {
  object Versions {
    val zio = "2.0.9"
    val circeVersion = "0.14.4"
    val jwtCirceVersion = "9.2.0"
    val zioPreludeVersion = "1.0.0-RC16"
    val castor = Dependencies.Versions.castor
  }

  private lazy val coreJwtCirce = "io.circe" %% "circe-core" % Versions.circeVersion
  private lazy val genericJwtCirce = "io.circe" %% "circe-generic" % Versions.circeVersion
  private lazy val parserJwtCirce = "io.circe" %% "circe-parser" % Versions.circeVersion

  private lazy val circeJsonSchema = ("net.reactivecore" %% "circe-json-schema" % "0.3.0")
    .cross(CrossVersion.for3Use2_13)
    .exclude("io.circe", "circe-core_2.13")
    .exclude("io.circe", "circe-generic_2.13")
    .exclude("io.circe", "circe-parser_2.13")

  private lazy val jwtCirce = "com.github.jwt-scala" %% "jwt-circe" % Versions.jwtCirceVersion

  private lazy val zio = "dev.zio" %% "zio" % Versions.zio
  private lazy val zioPrelude = "dev.zio" %% "zio-prelude" % Versions.zioPreludeVersion

  private lazy val nimbusJoseJwt = "com.nimbusds" % "nimbus-jose-jwt" % "9.30.2"

  private lazy val zioTest = "dev.zio" %% "zio-test" % Versions.zio % Test
  private lazy val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Versions.zio % Test
  private lazy val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % Versions.zio % Test

  private lazy val castorCore = "io.iohk.atala" %% "castor-core" % Versions.castor

  // Dependency Modules
  private lazy val zioDependencies: Seq[ModuleID] = Seq(zio, zioPrelude, zioTest, zioTestSbt, zioTestMagnolia)
  private lazy val circeDependencies: Seq[ModuleID] = Seq(coreJwtCirce, genericJwtCirce, parserJwtCirce)
  private lazy val baseDependencies: Seq[ModuleID] =
    circeDependencies ++ zioDependencies :+ jwtCirce :+ circeJsonSchema :+ nimbusJoseJwt :+ castorCore

  // Project Dependencies
  lazy val polluxVcJwtDependencies: Seq[ModuleID] = baseDependencies
}
