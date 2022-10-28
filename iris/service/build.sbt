import Dependencies._
import sbt.Keys.testFrameworks
import sbtghpackages.GitHubPackagesPlugin.autoImport._

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Iris gRPC specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../api"

def commonProject(project: Project): Project =
  project.settings(
    version := "0.1.0",
    organization := "io.iohk.atala",
    scalaVersion := "3.2.0",
    githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
    resolvers += Resolver
      .githubPackages("input-output-hk", "atala-prism-sdk"),
    // Needed for Kotlin coroutines that support new memory management mode
    resolvers +=
      "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
  )

// Project definitions
lazy val root = commonProject(project)
  .in(file("."))
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
    name := "iris-server",
    libraryDependencies ++= serverDependencies,
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerRepository := Some("atala-prism.io"),
    Docker / dockerUsername := Some("input-output-hk"),
    // Docker / packageName := s"atala-prism/${packageName.value}",
    dockerExposedPorts := Seq(8081),
    dockerBaseImage := "openjdk:11"
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(core, sql)
