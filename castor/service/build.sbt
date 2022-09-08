import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

lazy val root = project
  .in(file("."))
  .aggregate(models, `http-server`)

lazy val models = project
  .in(file("models"))
  .settings(name := "castor-models")

lazy val `http-server` = project
  .in(file("http-server"))
  .settings(
    name := "castor-http-server",
    libraryDependencies ++= baseDependencies ++ httpDependencies,
    Compile / guardrailTasks := List(
      ScalaServer(
        specPath = file("../api/http/castor-openapi-spec.yaml"),
        pkg = "io.iohk.atala.castor.server",
        framework = "http4s"
      )
    )
  )
  .dependsOn(models)
