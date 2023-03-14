import Dependencies._
import sbtghpackages.GitHubPackagesPlugin.autoImport._

// Custom keys
val apiBaseDirectory =
  settingKey[File]("The base directory for PrismAgent API specifications")

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.2",
    apiBaseDirectory := baseDirectory.value / "api",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec"),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    resolvers += Resolver.githubPackages("input-output-hk"),
    resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks"
  )
)

coverageDataDir := target.value / "coverage"

// Project definitions
lazy val root = project
  .in(file("."))
  .aggregate(`wallet-api`, server)

lazy val `wallet-api` = project
  .in(file("wallet-api"))
  .settings(
    name := "prism-agent-wallet-api",
    libraryDependencies ++= keyManagementDependencies
  )

lazy val server = project
  .in(file("server"))
  .settings(
    name := "prism-agent",
    fork := true,
    libraryDependencies ++= serverDependencies,
    Compile / mainClass := Some("io.iohk.atala.agent.server.MainApp"),
    // OpenAPI settings
    Compile / unmanagedResourceDirectories += apiBaseDirectory.value,
    Compile / sourceGenerators += openApiGenerateClasses,
    openApiGeneratorSpec := apiBaseDirectory.value / "http/prism-agent-openapi-spec.yaml",
    openApiGeneratorConfig := baseDirectory.value / "openapi/generator-config/config.yaml",
    openApiGeneratorImportMapping := Seq(
      "DidOperationType",
      "DidOperationStatus"
    )
      .map(model =>
        (model, s"io.iohk.atala.agent.server.http.model.OASModelPatches.$model")
      )
      .toMap,
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerUsername := Some("input-output-hk"),
    Docker / githubOwner := "atala-prism-building-blocks",
    Docker / dockerRepository := Some("ghcr.io"),
    dockerExposedPorts := Seq(8080, 8085, 8090),
    dockerBaseImage := "openjdk:11",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.iohk.atala.agent.server.buildinfo"
  )
  .enablePlugins(OpenApiGeneratorPlugin, JavaAppPackaging, DockerPlugin, BuildInfoPlugin)
  .dependsOn(`wallet-api`)

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  ReleaseStep(releaseStepTask(server / Docker / publish)),
  setNextVersion
)
