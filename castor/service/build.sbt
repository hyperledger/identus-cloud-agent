import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

lazy val root = project
  .in(file("."))
  .aggregate(models, core, `api-server`, worker)

lazy val models = project
  .in(file("models"))
  .settings(name := "castor-models")

lazy val core = project
  .in(file("core"))
  .settings(
    name := "castor-core",
    libraryDependencies ++= coreDependencies,
  )
  .dependsOn(models)

lazy val `api-server` = project
  .in(file("api-server"))
  .settings(
    name := "castor-api-server",
    libraryDependencies ++= httpServerDependencies,
    Compile / sourceGenerators += openApiGenerateClasses,
    openApiGeneratorSpec := baseDirectory.value / "../../api/http/castor-openapi-spec.yaml",
    openApiGeneratorConfig := baseDirectory.value / "openapi/generator-config/config.yaml"
  )
  .enablePlugins(OpenApiGeneratorPlugin)
  .dependsOn(core)

lazy val worker = project
  .in(file("worker"))
  .settings(
    name := "castor-worker",
    libraryDependencies ++= workerDependencies
  )
  .dependsOn(core)
