import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Castor API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../api"

// Project definitions
lazy val root = project
  .in(file("."))
  .aggregate(core, sql, server)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "castor-core",
    libraryDependencies ++= coreDependencies
  )

lazy val sql = project
  .in(file("sql"))
  .settings(
    name := "castor-sql",
    libraryDependencies ++= sqlDependencies
  )
  .dependsOn(core)

lazy val server = project
  .in(file("server"))
  .settings(
    name := "castor-server",
    libraryDependencies ++= apiServerDependencies,
    // OpenAPI settings
    Compile / sourceGenerators += openApiGenerateClasses,
    openApiGeneratorSpec := apiBaseDirectory.value / "http/prism-agent-openapi-spec.yaml",
    openApiGeneratorConfig := baseDirectory.value / "openapi/generator-config/config.yaml",
    openApiGeneratorImportMapping := Seq("DidType", "DidOperationType", "DidOperationStatus")
      .map(model => (model, s"io.iohk.atala.castor.server.http.OASModelPatches.$model"))
      .toMap,
  )
  .enablePlugins(OpenApiGeneratorPlugin)
  .dependsOn(core, sql)
