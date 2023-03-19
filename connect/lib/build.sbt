import Dependencies._

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.2",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec"),
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks",
    resolvers += Resolver.githubPackages("input-output-hk"),
    resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
)

coverageDataDir := target.value / "coverage"

// Project definitions
lazy val root = project
  .in(file("."))
  .settings(name := "connect")
  .aggregate(core, `sql-doobie`)
publish / skip := true // Do not publish the root

lazy val core = project
  .in(file("core"))
  .settings(
    name := "connect-core",
    libraryDependencies ++= coreDependencies,
    Test / publishArtifact := true
  )

lazy val `sql-doobie` = project
  .in(file("sql-doobie"))
  .settings(
    name := "connect-sql-doobie",
    libraryDependencies ++= sqlDoobieDependencies
  )
  .dependsOn(core % "compile->compile;test->test")

// Configure release process
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  publishArtifacts,
  setNextVersion
)
