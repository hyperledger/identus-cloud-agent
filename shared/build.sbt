/*
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._
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
        githubRepository := "atala-prism-building-blocks"
    )
)

coverageDataDir := target.value / "coverage"

SbtUtils.disablePlugins(publishConfigure) // SEE also SbtUtils.scala
lazy val publishConfigure: Project => Project = sys.env
  .get("PUBLISH_PACKAGES") match {
    case None    => _.disablePlugins(GitHubPackagesPlugin)
    case Some(_) => (p: Project) => p
}

sys.env
  .get("PUBLISH_PACKAGES") // SEE also plugin.sbt
  .map { _ =>
      println("### Configure release process ###")
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
  }
  .toSeq

lazy val root = (project in file("."))
  .configure(publishConfigure)
  .settings(
    organization := "io.iohk.atala",
    organizationName := "Input Output Global",
    buildInfoPackage := "io.iohk.atala.shared",
    name := "shared",
    crossPaths := false,
    libraryDependencies ++= dependencies
  )
   .enablePlugins(BuildInfoPlugin)

 */
