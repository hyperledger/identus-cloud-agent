val tapirVersion = "1.0.1"

lazy val rootProject = (project in file(".")).settings(
  Seq(
    name := "prism-mediator",
    version := "0.1.0-SNAPSHOT",
    organization := "io.iohk.atala",
    scalaVersion := "2.13.8",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "org.http4s" %% "http4s-blaze-server" % "0.23.12",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.11",
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
      "dev.zio" %% "zio-test" % "2.0.0" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.0.0" % Test,
      "com.softwaremill.sttp.client3" %% "circe" % "3.7.1" % Test,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % "1.0.0-M9",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.19.0-M1",
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % "0.2.1"
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
)
