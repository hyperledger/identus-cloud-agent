import sbt._

object Dependencies {
  object Versions {
    val circeVersion = "0.14.3"
    val jwtCirceVersion = "9.1.1"
    val zioPreludeVersion = "1.0.0-RC15"
  }

  private lazy val coreJwtCirce = ("io.circe" %% "circe-core" % Versions.circeVersion).cross(CrossVersion.for3Use2_13)
  private lazy val genericJwtCirce =
    ("io.circe" %% "circe-generic" % Versions.circeVersion).cross(CrossVersion.for3Use2_13)
  private lazy val parserJwtCirce =
    ("io.circe" %% "circe-parser" % Versions.circeVersion).cross(CrossVersion.for3Use2_13)

  private lazy val circeJsonSchema =
    ("net.reactivecore" %% "circe-json-schema" % "0.3.0").cross(CrossVersion.for3Use2_13)

  private lazy val jwtCirce =
    ("com.github.jwt-scala" %% "jwt-circe" % Versions.jwtCirceVersion).cross(CrossVersion.for3Use2_13)

  private lazy val zioPrelude = "dev.zio" %% "zio-prelude" % Versions.zioPreludeVersion

  private lazy val test = "org.scalameta" %% "munit" % "0.7.29" % Test

  // Dependency Modules
  private lazy val zioDependencies: Seq[ModuleID] = Seq(zioPrelude)
  private lazy val circeDependencies: Seq[ModuleID] = Seq(coreJwtCirce, genericJwtCirce, parserJwtCirce)
  private lazy val baseDependencies: Seq[ModuleID] =
    circeDependencies ++ zioDependencies :+ jwtCirce :+ circeJsonSchema :+ test

  // Project Dependencies
  lazy val polluxVcJwtDependencies: Seq[ModuleID] = baseDependencies
}