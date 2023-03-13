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
  )
)

coverageDataDir := target.value / "coverage"

SbtUtils.disablePlugins(publishConfigure) // SEE also SbtUtils.scala
lazy val publishConfigure: Project => Project = sys.env
  .get("PUBLISH_PACKAGES") match {
  case None    => _.disablePlugins(GitHubPackagesPlugin)
  case Some(_) => (p: Project) => p
}

sys.env
  .get("PUBLISH_PACKAGES") // SEE also plugin.sbt
  .map { _ =>
    println("### Configure release process ###")
    import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
    ThisBuild / releaseUseGlobalVersion := false
    ThisBuild / githubOwner := "input-output-hk"
    ThisBuild / githubRepository := "atala-prism-building-blocks"
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      ReleaseStep(releaseStepTask(server / Docker / publish)),
      setNextVersion
    )
  }
  .toSeq

val commonSettings = Seq(
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  resolvers += Resolver.githubPackages("input-output-hk"),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven"
)

// Project definitions
lazy val root = project
  .in(file("."))
  .configure(publishConfigure)
  .settings(commonSettings)
  .aggregate(`wallet-api`, server)

lazy val `wallet-api` = project
  .in(file("wallet-api"))
  .configure(publishConfigure)
  .settings(commonSettings)
  .settings(
    name := "prism-agent-wallet-api",
    libraryDependencies ++= keyManagementDependencies
  )

lazy val server = project
  .in(file("server"))
  .configure(publishConfigure)
  .settings(commonSettings)
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
