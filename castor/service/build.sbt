import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"

lazy val root = project
  .in(file("."))
  .aggregate(`castor-models`)

lazy val `castor-models` = project
  .in(file("castor-models"))
  .settings(
    name := "castor-models",
    libraryDependencies ++= Nil
  )
