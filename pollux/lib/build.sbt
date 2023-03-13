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
    Test / envVars := Map("TZ" -> "UTC")
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
  testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  resolvers += Resolver.githubPackages("input-output-hk"),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven"
)

// Project definitions
lazy val root = project
  .in(file("."))
  .configure(publishConfigure)
  .settings(commonSettings)
  .settings(name := "pollux-root")
  .aggregate(core, `sql-doobie`, vcJWT)
publish / skip := true //Do not publish the root

lazy val vcJWT = project
  .in(file("vc-jwt"))
  .configure(publishConfigure)
  .settings(commonSettings)
  .settings(
    name := "pollux-vc-jwt",
    libraryDependencies ++= polluxVcJwtDependencies
  )

lazy val core = project
  .in(file("core"))
  .configure(publishConfigure)
  .settings(commonSettings)
  .settings(
    name := "pollux-core",
    libraryDependencies ++= coreDependencies
  )
  .dependsOn(vcJWT)

lazy val `sql-doobie` = project
  .in(file("sql-doobie"))
  .configure(publishConfigure)
  .settings(commonSettings)
  .settings(
    name := "pollux-sql-doobie",
    libraryDependencies ++= sqlDoobieDependencies
  )
  .dependsOn(core % "compile->compile;test->test")
