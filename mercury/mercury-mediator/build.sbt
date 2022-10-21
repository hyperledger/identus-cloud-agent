inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.0",
    fork := true,
    run / connectInput := true,
    versionScheme := Some("semver-spec"),
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks",
    githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")
  )
)

// Custom keys
val apiBaseDirectory =
  settingKey[File]("The base directory for Castor API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / ".." / "api"
ThisBuild / resolvers += Resolver.githubPackages(
  "input-output-hk",
  "atala-prism-building-blocks"
)

lazy val V = new {
  val munit = "1.0.0-M6" // "0.7.29"
  val munitZio = "0.1.1"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.0.2"
  val zioLogging = "2.0.0"
  val zioJson = "0.3.0"
  val zioHttp = "2.0.0-RC10" // "2.0.0-RC11" TODO

  // https://mvnrepository.com/artifact/io.circe/circe-core
  val circe = "0.14.2"

  val tapir = "1.0.3"

  val mercury = "0.2.0"
}

/** Dependencies */
lazy val D = new {

  val mercuryModels =
    Def.setting("io.iohk.atala" %% "mercury-data-models" % V.mercury)
  // val mercury10 = Def.setting("io.iohk.atala" %% "mercury-agent-didscala" % V.mercury)
  // val mercury11 = Def.setting("io.iohk.atala" %% "mercury-agent-core" % V.mercury)
  val mercuryAgent =
    Def.setting("io.iohk.atala" %% "mercury-agent-didcommx" % V.mercury)
  // val mercury2 = Def.setting("io.iohk.atala" %% "protocol-outofband-login" % V.mercury) //FIXME
  val mercury4 = Def.setting("io.iohk.atala" %% "mercury-resolver" % V.mercury)
  val mercury3 =
    Def.setting("io.iohk.atala" %% "mercury-protocol-routing-2_0" % V.mercury)
  val mercury5 = Def.setting(
    "io.iohk.atala" %% "mercury-protocol-coordinate-mediation" % V.mercury
  )
  val mercury6 =
    Def.setting("io.iohk.atala" %% "mercury-protocol-invitation" % V.mercury)
  val mercury7 =
    Def.setting("io.iohk.atala" %% "mercury-protocol-did-exchange" % V.mercury)
  val mercury8 =
    Def.setting("io.iohk.atala" %% "mercury-protocol-mailbox" % V.mercury)
  val mercury9 =
    Def.setting("io.iohk.atala" %% "mercury-protocol-connection" % V.mercury)

  val zio = Def.setting("dev.zio" %% "zio" % V.zio)
  val zioStreams = Def.setting("dev.zio" %% "zio-streams" % V.zio)
  val zioLog = Def.setting("dev.zio" %% "zio-logging" % V.zioLogging)
  val zioSLF4J = Def.setting("dev.zio" %% "zio-logging-slf4j" % V.zioLogging)
  val zioJson = Def.setting("dev.zio" %% "zio-json" % V.zioJson)

  // val zioHttp = Def.setting("dev.zio" %% "zio-http" % V.zioHttp) // FIXME USE THIS ONE
  val zioHttp = Def.setting(
    "io.d11" %% "zhttp" % V.zioHttp
  ) // REMOVE (this is the old name)

  val circeCore = Def.setting("io.circe" %% "circe-core" % V.circe)
  val circeGeneric = Def.setting("io.circe" %% "circe-generic" % V.circe)
  val circeParser = Def.setting("io.circe" %% "circe-parser" % V.circe)

  // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit = Def.setting("org.scalameta" %% "munit" % V.munit % Test)
  // For munit zio https://github.com/poslegm/munit-zio
  val munitZio =
    Def.setting("com.github.poslegm" %% "munit-zio" % V.munitZio % Test)

}

// ################
// ### Mediator ###
// ################

/** The mediator service */
lazy val mediator = project
  .in(file("."))
  .settings(name := "mercury-mediator")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies += D.munitZio.value)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % "0.23.12",
      "ch.qos.logback" % "logback-classic" % "1.2.11",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % V.tapir,
      // "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % V.tapir % Test,
      // "com.softwaremill.sttp.client3" %% "circe" % "3.7.1" % Test,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % "1.0.0-M9",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.19.0-M4",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.0.3", // This helps with Arrow Functions. But swagger is just a pain!
      "com.softwaremill.sttp.tapir" %% "tapir-redoc-http4s" % "0.19.0-M4",
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % "0.2.1",
      D.mercuryModels.value,
      D.mercuryAgent.value,
      D.mercury3.value,
      D.mercury4.value,
      D.mercury5.value,
      D.mercury6.value,
      D.mercury7.value,
      D.mercury8.value,
      D.mercury9.value
    ),
    Compile / unmanagedResourceDirectories += apiBaseDirectory.value,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
    // Docker / maintainer := "atala-coredid@iohk.io",
    // Docker / dockerRepository := Some("atala-prism.io"),
    // dockerExposedPorts := Seq(8080),
    // dockerBaseImage := "openjdk:11"
  )
// .enablePlugins(JavaAppPackaging, DockerPlugin)
// .dependsOn(
//   agentDidcommx,
//   resolver,
//   protocolInvitation,
//   protocolConnection,
//   protocolDidExchange,
//   protocolMercuryMailbox,
//   protocolReportProblem,
//   protocolRouting,
//   protocolIssueCredential
// )
