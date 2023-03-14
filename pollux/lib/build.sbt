import Dependencies._
import Dependencies_VC_JWT._ //TODO REMOVE

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.2",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec"),
    Compile / javaOptions += "-Dquill.macro.log=false -Duser.timezone=UTC",
    Test / javaOptions += "-Dquill.macro.log=false -Duser.timezone=UTC -Xms2048m -Xmx2048m -Xss16M",
    Test / envVars := Map("TZ" -> "UTC"),
    testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
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
  .settings(name := "pollux-root")
  .aggregate(core, `sql-doobie`, vcJWT)
publish / skip := true // do not publish the root

lazy val vcJWT = project
  .in(file("vc-jwt"))
  .settings(
    name := "pollux-vc-jwt",
    libraryDependencies ++= polluxVcJwtDependencies
  )

lazy val core = project
  .in(file("core"))
  .settings(
    name := "pollux-core",
    libraryDependencies ++= coreDependencies
  )
  .dependsOn(vcJWT)

lazy val `sql-doobie` = project
  .in(file("sql-doobie"))
  .settings(
    name := "pollux-sql-doobie",
    libraryDependencies ++= sqlDoobieDependencies
  )
  .dependsOn(core % "compile->compile;test->test")

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
