import Dependencies._
import sbt.Keys.testFrameworks
import sbtghpackages.GitHubPackagesPlugin.autoImport._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Iris API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../api"

val commonSettings = Seq(
  githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
  resolvers += Resolver.githubPackages("input-output-hk", "atala-prism-sdk"),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers +=
    "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
)

// Project definitions
lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .aggregate(core, sql, server)

lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "iris-core",
    libraryDependencies ++= coreDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    // gRPC settings
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    Compile / PB.protoSources := Seq(apiBaseDirectory.value / "grpc")
  )

lazy val sql = project
  .in(file("sql"))
  .settings(commonSettings)
  .settings(
    name := "iris-sql",
    libraryDependencies ++= sqlDependencies
  )
  .dependsOn(core)

lazy val server = project
  .in(file("server"))
  .settings(commonSettings)
  .settings(
    name := "iris-server",
    libraryDependencies ++= serverDependencies,
  )
  .dependsOn(core, sql)
