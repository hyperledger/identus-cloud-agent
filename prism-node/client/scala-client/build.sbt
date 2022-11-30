import Dependencies._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.1",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec")
  )
)

val commonSettings = Seq(
  githubOwner := "input-output-hk",
  githubRepository := "atala-prism-building-blocks",
  githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN")
)

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Node 2 API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../../api"

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    name := "prism-node-client",
    libraryDependencies ++= rootDependencies,
    // gRPC settings
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    Compile / PB.protoSources := Seq(apiBaseDirectory.value / "grpc")
  )

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
