import Dependencies._

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

// Project definitions
lazy val root = project
  .in(file("."))
  .configure(publishConfigure)
  .settings(
    name := "castor-root",
  )
  .settings(publish / skip := true)
  .aggregate(core, `sql-doobie`)

lazy val core = project
  .in(file("core"))
  .configure(publishConfigure)
  .settings(
    name := "castor-core",
    libraryDependencies ++= coreDependencies
  )

lazy val `sql-doobie` = project
  .in(file("sql-doobie"))
  .configure(publishConfigure)
  .settings(
    name := "castor-sql-doobie",
    libraryDependencies ++= sqlDoobieDependencies
  )
  .dependsOn(core)
