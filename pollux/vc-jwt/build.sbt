import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

lazy val root = project
  .in(file("."))
  .settings(
    name := "pollux-vc-jwt",
    libraryDependencies ++= polluxVcJwtDependencies
  )
