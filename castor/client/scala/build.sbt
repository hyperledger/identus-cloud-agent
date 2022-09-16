import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Castor API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../../api"

// Project definitions
lazy val root = project
  .in(file("."))
  .settings(
    name := "castor-client",
    libraryDependencies ++= clientDependencies,
    // gRPC settings
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    Compile / PB.protoSources := Seq(apiBaseDirectory.value / "grpc")
  )
