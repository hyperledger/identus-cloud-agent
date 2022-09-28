import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

// Project definitions
lazy val root = project
  .in(file("."))
  .aggregate(core, `sql-doobie`)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "castor-core",
    libraryDependencies ++= coreDependencies
  )

lazy val `sql-doobie` = project
  .in(file("sql-doobie"))
  .settings(
    name := "castor-sql-doobie",
    libraryDependencies ++= sqlDoobieDependencies
  )
  .dependsOn(core)
