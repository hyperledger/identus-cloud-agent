import Dependencies._

ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

val commonSettings = Seq(
  githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
  resolvers += Resolver.githubPackages("input-output-hk", "atala-prism-sdk"),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
)

// Project definitions
lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .aggregate(core, `sql-doobie`)

lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "pollux-core",
    libraryDependencies ++= coreDependencies
  )

lazy val `sql-doobie` = project
  .in(file("sql-doobie"))
  .settings(commonSettings)
  .settings(
    name := "pollux-sql-doobie",
    libraryDependencies ++= sqlDoobieDependencies
  )
  .dependsOn(core)
