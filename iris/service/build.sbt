import Dependencies._
import sbt.Keys.testFrameworks
import sbtghpackages.GitHubPackagesPlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Iris gRPC specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../api"

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.0",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec"),
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks",
    githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN")
  )
)

def commonProject(project: Project): Project =
  project.settings(
    githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
    versionScheme := Some("semver-spec"),
    resolvers += Resolver
      .githubPackages("input-output-hk"),
    // Needed for Kotlin coroutines that support new memory management mode
    resolvers +=
      "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
  )

// Project definitions
lazy val root = project
  .in(file("."))
  .settings(
    name := "iris-service-root"
  )
  .aggregate(core, sql, server)

lazy val core = commonProject(project)
  .in(file("core"))
  .settings(
    name := "iris-core",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= coreDependencies,
    // gRPC settings
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    Compile / PB.protoSources := Seq(apiBaseDirectory.value / "grpc")
  )

lazy val sql = commonProject(project)
  .in(file("sql"))
  .settings(
    name := "iris-sql",
    libraryDependencies ++= sqlDependencies
  )
  .dependsOn(core)

lazy val server = commonProject(project)
  .in(file("server"))
  .settings(
    name := "iris-service",
    libraryDependencies ++= serverDependencies,
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerUsername := Some("input-output-hk"),
    Docker / githubOwner := "atala-prism-building-blocks",
    Docker / dockerRepository := Some("ghcr.io"),
    Docker / dockerUpdateLatest := true,
    dockerExposedPorts := Seq(8081),
    dockerBaseImage := "openjdk:11"
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(core, sql)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  ReleaseStep(releaseStepTask(server / Docker / publish)),
  setNextVersion
)
