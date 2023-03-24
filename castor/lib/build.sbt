import Dependencies._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.2",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec"),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    releaseUseGlobalVersion := false,
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks",
    resolvers += Resolver.githubPackages("input-output-hk"),
    resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven"
  )
)

coverageDataDir := target.value / "coverage"

// Project definitions
lazy val root = project
  .in(file("."))
  .settings(
    name := "castor-root",
  )
  .settings(publish / skip := true)
  .aggregate(core, `sql-doobie`)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "castor-core",
    libraryDependencies ++= coreDependencies
  )

lazy val `sql-doobie` = project
  .in(file("sql-doobie"))
  .settings(
    name := "castor-sql-doobie",
    libraryDependencies ++= sqlDoobieDependencies
  )
  .dependsOn(core)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  publishArtifacts,
  setNextVersion
)
