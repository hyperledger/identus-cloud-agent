import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import Dependencies._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

inThisBuild(
    Seq(
        organization := "io.iohk.atala",
        scalaVersion := "3.2.1",
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
)

lazy val root = (project in file("."))
  .settings(
    organization := "io.iohk.atala",
    organizationName := "Input Output Global",
    buildInfoPackage := "io.iohk.atala.shared",
    name := "shared",
    crossPaths := false,
    libraryDependencies ++= dependencies
  )
   .settings(commonSettings)
   .enablePlugins(BuildInfoPlugin)

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
