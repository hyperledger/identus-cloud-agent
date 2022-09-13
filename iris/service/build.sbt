import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Iris API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../api"

// Project definitions
lazy val root = project
  .in(file("."))
  .aggregate(core, sql, `api-server`)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "iris-core",
    libraryDependencies ++= coreDependencies
  )

lazy val sql = project
  .in(file("sql"))
  .settings(
    name := "iris-sql",
    libraryDependencies ++= sqlDependencies
  )
  .dependsOn(core)

lazy val `api-server` = project
  .in(file("api-server"))
  .settings(
    name := "iris-api-server",
    libraryDependencies ++= apiServerDependencies,
    // gRPC settings
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    Compile / PB.protoSources := Seq(apiBaseDirectory.value / "grpc")
  )
  .dependsOn(core, sql)
