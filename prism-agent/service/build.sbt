import Dependencies._
import sbtghpackages.GitHubPackagesPlugin.autoImport._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for PrismAgent API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../api"

val commonSettings = Seq(
  githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
  resolvers += Resolver.githubPackages("input-output-hk", "atala-prism-sdk"),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
)

// Project definitions
lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .aggregate(server)

lazy val server = project
  .in(file("server"))
  .settings(commonSettings)
  .settings(
    name := "agent-server",
    libraryDependencies ++= apiServerDependencies,
    // OpenAPI settings
    Compile / sourceGenerators += openApiGenerateClasses,
    openApiGeneratorSpec := apiBaseDirectory.value / "http/prism-agent-openapi-spec.yaml",
    openApiGeneratorConfig := baseDirectory.value / "openapi/generator-config/config.yaml",
    openApiGeneratorImportMapping := Seq("DidType", "DidOperationType", "DidOperationStatus")
      .map(model => (model, s"io.iohk.atala.agent.server.http.OASModelPatches.$model"))
      .toMap,
  )
  .enablePlugins(OpenApiGeneratorPlugin)
