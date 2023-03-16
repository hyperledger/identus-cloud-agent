import Dependencies._

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.2",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec"),
    releaseUseGlobalVersion := false,
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks",
    resolvers += Resolver.githubPackages("input-output-hk"),
  )
)

coverageDataDir := target.value / "coverage"

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Iris API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../../api"

lazy val root = project
  .in(file("."))
  .settings(
    name := "iris-client",
    libraryDependencies ++= rootDependencies,
    // gRPC settings
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    Compile / PB.protoSources := Seq(apiBaseDirectory.value / "grpc")
  )

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
