import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / organization := "io.iohk.atala"

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Pollux API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../api"

// Project definitions
lazy val root = project
  .in(file("."))
  .aggregate(server)

lazy val server = project
  .in(file("server"))
  .settings(
    name := "pollux-server",
    libraryDependencies ++= apiServerDependencies,
    // OpenAPI settings
    Compile / unmanagedResourceDirectories += apiBaseDirectory.value / "http",
    Compile / sourceGenerators += openApiGenerateClasses,
    openApiGeneratorSpec := apiBaseDirectory.value / "http/pollux-openapi-spec.yaml",
    openApiGeneratorConfig := baseDirectory.value / "openapi/generator-config/config.yaml",
    // gRPC settings
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    Compile / PB.protoSources := Seq(apiBaseDirectory.value / "grpc"),
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerRepository := Some("atala-prism.io"),
    // Docker / packageName := s"atala-prism/${packageName.value}",
    dockerExposedPorts := Seq(8080),
    dockerBaseImage := "openjdk:11"
  )
  .enablePlugins(OpenApiGeneratorPlugin, JavaAppPackaging, DockerPlugin)
