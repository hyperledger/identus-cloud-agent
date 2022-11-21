import Dependencies._
import Dependencies_VC_JWT._ //TODO REMOVE
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

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

val commonSettings = Seq(
  githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
  resolvers += Resolver.githubPackages("input-output-hk"),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven"
)

// Project definitions
lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(name := "pollux-root")
  .aggregate(core, `sql-doobie`, vcJWT)
publish / skip := true //Do not publish the root

lazy val vcJWT = project
  .in(file("vc-jwt"))
  .settings(commonSettings)
  .settings(
    name := "pollux-vc-jwt",
    libraryDependencies ++= polluxVcJwtDependencies
  )

lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "pollux-core",
    libraryDependencies ++= coreDependencies
  )
  .dependsOn(vcJWT)

lazy val `sql-doobie` = project
  .in(file("sql-doobie"))
  .settings(commonSettings)
  .settings(
    name := "pollux-sql-doobie",
    libraryDependencies ++= sqlDoobieDependencies
  )
  .dependsOn(core)

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
