import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import Dependencies._
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
        githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")
    )
)

lazy val root = (project in file("."))
  .settings(
    organization := "io.iohk.atala",
    organizationName := "Input Output Global",
    buildInfoPackage := "io.iohk.atala.shared",
    name := "shared",
    crossPaths := false,
    libraryDependencies ++= dependencies
  ).enablePlugins(BuildInfoPlugin)

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
