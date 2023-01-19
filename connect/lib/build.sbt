import Dependencies._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.2",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec"),
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks",
    githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN")
  )
)

val commonSettings = Seq(
  githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
  resolvers += Resolver.githubPackages("input-output-hk"),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
)

// Project definitions
lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(name := "connect")
  .aggregate(core, `sql-doobie`)
publish / skip := true //Do not publish the root

lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "connect-core",
    libraryDependencies ++= coreDependencies,
    Test / publishArtifact := true
  )

lazy val `sql-doobie` = project
  .in(file("sql-doobie"))
  .settings(commonSettings)
  .settings(
    name := "connect-sql-doobie",
    libraryDependencies ++= sqlDoobieDependencies
  )
  .dependsOn(core % "compile->compile;test->test")

// ### ReleaseStep ###
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  publishArtifacts,
  setNextVersion
)
