import Dependencies._
import sbtghpackages.GitHubPackagesPlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

// Custom keys
val apiBaseDirectory =
  settingKey[File]("The base directory for PrismAgent API specifications")

inThisBuild(
  Seq(
    maxErrors := 5,
    organization := "io.iohk.atala",
    scalaVersion := "3.2.1",
    apiBaseDirectory := baseDirectory.value / "api",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec"),
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks",
    githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN")
  )
)

val commonSettings = Seq(
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
  resolvers += Resolver.githubPackages("input-output-hk"),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven"
)

// Project definitions
lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .aggregate(`wallet-api`, server)

lazy val `wallet-api` = project
  .in(file("wallet-api"))
  .settings(commonSettings)
  .settings(
    name := "prism-agent-wallet-api",
    libraryDependencies ++= keyManagementDependencies
  )

lazy val server = project
  .in(file("server"))
  .settings(commonSettings)
  .settings(
    name := "prism-agent",
    fork := true,
    libraryDependencies ++= serverDependencies,
    Compile / mainClass := Some("io.iohk.atala.agent.server.Main"),
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
    dockerExposedPorts := Seq(8080, 8085),
    dockerBaseImage := "openjdk:11"
  )
  .enablePlugins(OpenApiGeneratorPlugin, JavaAppPackaging, DockerPlugin)
  .dependsOn(`wallet-api`)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  ReleaseStep(releaseStepTask(server / Docker / publish)),
  setNextVersion
)
