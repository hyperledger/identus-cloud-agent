import Dependencies._

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.2",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec"),
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
    ThisBuild / releaseUseGlobalVersion := false
    ThisBuild / githubOwner := "input-output-hk"
    ThisBuild / githubRepository := "atala-prism-building-blocks"
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

val commonSettings = Seq(
  resolvers += Resolver.githubPackages("input-output-hk"),
)

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Iris API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../../api"

lazy val root = project
  .in(file("."))
  .configure(publishConfigure)
  .settings(
    name := "iris-client",
    libraryDependencies ++= rootDependencies,
    // gRPC settings
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    Compile / PB.protoSources := Seq(apiBaseDirectory.value / "grpc")
  )
  .settings(commonSettings)
