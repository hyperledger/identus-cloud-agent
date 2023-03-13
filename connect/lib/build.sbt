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
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
)

// Project definitions
lazy val root = project
  .in(file("."))
  .configure(publishConfigure)
  .settings(commonSettings)
  .settings(name := "connect")
  .aggregate(core, `sql-doobie`)
publish / skip := true //Do not publish the root

lazy val core = project
  .in(file("core"))
  .configure(publishConfigure)
  .settings(commonSettings)
  .settings(
    name := "connect-core",
    libraryDependencies ++= coreDependencies,
    Test / publishArtifact := true
  )

lazy val `sql-doobie` = project
  .in(file("sql-doobie"))
  .configure(publishConfigure)
  .settings(commonSettings)
  .settings(
    name := "connect-sql-doobie",
    libraryDependencies ++= sqlDoobieDependencies
  )
  .dependsOn(core % "compile->compile;test->test")
