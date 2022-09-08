import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

lazy val root = project
  .in(file("."))
  .aggregate(models, `http-server`)

lazy val models = project
  .in(file("models"))
  .settings(name := "castor-models")

lazy val `http-server` = project
  .in(file("http-server"))
  .settings(
    name := "castor-http-server",
    libraryDependencies ++= baseDependencies ++ akkaHttpDependencies,
    Compile / sourceGenerators += openApiGenerateClasses,
    openApiGeneratorSpec := baseDirectory.value / "../../api/http/castor-openapi-spec.yaml",
    openApiGeneratorConfig := baseDirectory.value / "openapi/generator-config/config.yaml"
  )
  .enablePlugins(OpenApiGeneratorPlugin)
  .dependsOn(models)
