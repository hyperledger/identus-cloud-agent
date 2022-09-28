import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import Dependencies._

ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "3.1.3"

lazy val root = (project in file("."))
  .settings(
    organization := "io.iohk.atala",
    organizationName := "Input Output HK",
    buildInfoPackage := "io.iohk.atala.shared",
    name := "shared",
    crossPaths := false,
    libraryDependencies ++= dependencies
  ).enablePlugins(BuildInfoPlugin)
