val tapirVersion = "1.0.3"
val VERSION = "0.1.0-SNAPSHOT"

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.0"
  )
)

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Castor API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / ".." / "api"

val useDidLib = false
def didScalaAUX =
  if (useDidLib) (libraryDependencies += D.didScala.value)
  else (libraryDependencies ++= Seq())

lazy val V = new {
  val munit = "1.0.0-M6" // "0.7.29"
  val munitZio = "0.1.1"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.0.0"
  val zioJson = "0.3.0-RC10"

  // https://mvnrepository.com/artifact/io.circe/circe-core
  val circe = "0.14.2"
}

/** Dependencies */
lazy val D = new {
  val zio = Def.setting("dev.zio" %% "zio" % V.zio)
  val zioStreams = Def.setting("dev.zio" %% "zio-streams" % V.zio)
  val zioLog = Def.setting("dev.zio" %% "zio-logging" % V.zio)
  val zioSLF4J = Def.setting("dev.zio" %% "zio-logging-slf4j" % V.zio)
  val zioJson = Def.setting("dev.zio" %% "zio-json" % V.zioJson)

  val circeCore = Def.setting("io.circe" %% "circe-core" % V.circe)
  val circeGeneric = Def.setting("io.circe" %% "circe-generic" % V.circe)
  val circeParser = Def.setting("io.circe" %% "circe-parser" % V.circe)

  // Test DID comm
  val didcommx = Def.setting("org.didcommx" % "didcomm" % "0.3.1")
  val peerDidcommx = Def.setting("org.didcommx" % "peerdid" % "0.3.0")
  val didScala = Def.setting("app.fmgp" %% "did" % "0.0.0+74-691ada28+20220902-0934-SNAPSHOT")

  val jwk = Def.setting("com.nimbusds" % "nimbus-jose-jwt" % "9.25.4")

  // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit = Def.setting("org.scalameta" %% "munit" % V.munit % Test)
  // For munit zio https://github.com/poslegm/munit-zio
  val munitZio = Def.setting("com.github.poslegm" %% "munit-zio" % V.munitZio % Test)

}

// #########################
// ### Models & Services ###
// #########################

/** Just data models and interfaces of service.
  *
  * This module must not depend on external libraries!
  */
lazy val models = project
  .in(file("models"))
  .settings(name := "mercury-data-models", version := VERSION)
  .settings(
    libraryDependencies ++= Seq(D.zio.value),
    libraryDependencies ++= Seq(
      D.circeCore.value,
      D.circeGeneric.value,
      D.circeParser.value
    ), // TODO try to remove this from this module
    libraryDependencies += D.didcommx.value, // FIXME REMOVE almost done
    didScalaAUX, // D.didScala.value, // Just the data models
  )

// #################
// ### Protocols ###
// #################

lazy val protocolConnection = project
  .in(file("protocol-connection"))
  .settings(name := "mercury-protocol-connection", version := VERSION)
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .dependsOn(models, protocolInvitation)

lazy val protocolCoordinateMediation = project
  .in(file("protocol-coordinate-mediation"))
  .settings(name := "mercury-protocol-coordinate-mediation", version := VERSION)
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(models)

lazy val protocolInvitation = project
  .in(file("protocol-invitation"))
  .settings(name := "mercury-protocol-invitation", version := VERSION)
  .settings(libraryDependencies += D.zio.value)
  .settings(
    libraryDependencies ++= Seq(
      D.circeCore.value,
      D.circeGeneric.value,
      D.circeParser.value,
      D.munit.value,
      D.munitZio.value
    )
  )
  .dependsOn(models)

lazy val protocolDidExchange = project
  .in(file("protocol-did-exchange"))
  .settings(name := "mercury-protocol-did-exchange", version := VERSION)
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .dependsOn(models, protocolInvitation)

lazy val protocolMercuryMailbox = project
  .in(file("protocol-mercury-mailbox"))
  .settings(name := "mercury-protocol-mailbox", version := VERSION)
  .settings(libraryDependencies += D.zio.value)
  .dependsOn(models, protocolInvitation, protocolRouting)

lazy val protocolReportProblem = project
  .in(file("protocol-report-problem"))
  .settings(name := "protocol-report_problem", version := VERSION)
  .dependsOn(models)

lazy val protocolRouting = project
  .in(file("protocol-routing"))
  .settings(name := "mercury-protocol-routing-2_0", version := VERSION)
  .settings(libraryDependencies += D.zio.value)
  .dependsOn(models)

// ################
// ### Resolver ###
// ################

// TODO move stuff to the models module
lazy val resolver = project // maybe merge into models
  .in(file("resolver"))
  .settings(name := "mercury-resolver", version := VERSION)
  .settings(
    libraryDependencies ++= Seq(
      D.didcommx.value,
      D.peerDidcommx.value,
      D.munit.value,
      D.munitZio.value,
      D.jwk.value,
      "org.jetbrains.kotlin" % "kotlin-runtime" % "1.2.71",
      "org.jetbrains.kotlin" % "kotlin-stdlib" % "1.7.10",
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(models)

// ##############
// ### Agents ###
// ##############

lazy val agent = project // maybe merge into models
  // .in(file("agent-generic"))
  // .settings(name := "mercury-agent-generic", version := VERSION)
  .settings(libraryDependencies += "io.d11" %% "zhttp" % "2.0.0-RC10")
  .settings(libraryDependencies ++= Seq(D.zioLog.value)) // , D.zioSLF4J.value))
  .dependsOn(
    models,
    resolver,
    protocolCoordinateMediation,
    protocolInvitation,
    protocolRouting,
    protocolMercuryMailbox
  )

/** Demos agents and services implementation with didcommx */
lazy val agentDidcommx = project
  .in(file("agent-didcommx"))
  .settings(name := "mercury-agent-didcommx", version := VERSION)
  .settings(libraryDependencies += D.didcommx.value)
  .dependsOn(agent)

///** TODO Demos agents and services implementation with did-scala */
lazy val agentDidScala =
  project
    .in(file("agent-did-scala"))
    .settings(name := "mercury-agent-didscala", version := VERSION)
    .settings(
      didScalaAUX,
      if (useDidLib) (Compile / sources ++= Seq())
      else (Compile / sources := Seq()),
    )
    .dependsOn(agent)

// ################
// ### Mediator ###
// ################

/** The mediator service */
lazy val mediator = project
  .in(file("mediator"))
  .settings(name := "mercury-mediator", version := VERSION)
  .settings(
    libraryDependencies ++= Seq( // TODO cleanup
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "org.http4s" %% "http4s-blaze-server" % "0.23.12",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.11",
      // "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
      // "dev.zio" %% "zio-test" % "2.0.0" % Test,
      // "dev.zio" %% "zio-test-sbt" % "2.0.0" % Test,
      // "com.softwaremill.sttp.client3" %% "circe" % "3.7.1" % Test,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % "1.0.0-M9",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.19.0-M4",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.0.3", // This helps with Arrow Functions. But swagger is just a pain!
      "com.softwaremill.sttp.tapir" %% "tapir-redoc-http4s" % "0.19.0-M4",
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % "0.2.1",
      // D.didcommx.value,
      // "org.jetbrains.kotlin" % "kotlin-runtime" % "1.2.71",
      // "org.jetbrains.kotlin" % "kotlin-stdlib" % "1.7.10",
      // "com.google.code.gson" % "gson" % "2.9.1"
    ),
    Compile / unmanagedResourceDirectories += apiBaseDirectory.value,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerRepository := Some("atala-prism.io"),
    dockerExposedPorts := Seq(8080),
    dockerBaseImage := "openjdk:11"
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(agentDidcommx, resolver)
  .dependsOn(
    protocolInvitation,
    protocolConnection,
    protocolDidExchange,
    protocolMercuryMailbox,
    protocolReportProblem,
    protocolRouting,
  )
