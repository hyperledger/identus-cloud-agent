inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.2.2",
    fork := true,
    run / connectInput := true,
    releaseUseGlobalVersion := false,
    versionScheme := Some("semver-spec"),
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks"
  )
)

inThisBuild(
  Seq(
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-deprecation",
      "-unchecked",
      // TODO "-feature",
      // TODO "-Xfatal-warnings",
      // TODO "-Yexplicit-nulls",
      "-Ysafe-init",
    )
  )
)

lazy val V = new {
  val munit = "1.0.0-M6" // "0.7.29"
  val munitZio = "0.1.1"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.0.4"
  val zioLogging = "2.0.0"
  val zioJson = "0.3.0"
  val zioHttp = "2.0.0-RC11"

  // https://mvnrepository.com/artifact/io.circe/circe-core
  val circe = "0.14.2"

  val tapir = "1.0.3"
}

/** Dependencies */
lazy val D = new {
  val zio = Def.setting("dev.zio" %% "zio" % V.zio)
  val zioStreams = Def.setting("dev.zio" %% "zio-streams" % V.zio)
  val zioLog = Def.setting("dev.zio" %% "zio-logging" % V.zioLogging)
  val zioSLF4J = Def.setting("dev.zio" %% "zio-logging-slf4j" % V.zioLogging)
  val zioJson = Def.setting("dev.zio" %% "zio-json" % V.zioJson)

  val zioHttp = Def.setting("dev.zio" %% "zio-http" % "0.0.3")

  val circeCore = Def.setting("io.circe" %% "circe-core" % V.circe)
  val circeGeneric = Def.setting("io.circe" %% "circe-generic" % V.circe)
  val circeParser = Def.setting("io.circe" %% "circe-parser" % V.circe)

  // https://mvnrepository.com/artifact/org.didcommx/didcomm/0.3.2
  val didcommx = Def.setting("org.didcommx" % "didcomm" % "0.3.1")
  val peerDidcommx = Def.setting("org.didcommx" % "peerdid" % "0.3.0")
  val didScala = Def.setting("app.fmgp" %% "did" % "0.0.0+113-61efa271-SNAPSHOT")

  // https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt/9.16-preview.1
  val jwk = Def.setting("com.nimbusds" % "nimbus-jose-jwt" % "9.25.4")

  // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit = Def.setting("org.scalameta" %% "munit" % V.munit % Test)
  // For munit zio https://github.com/poslegm/munit-zio
  val munitZio = Def.setting("com.github.poslegm" %% "munit-zio" % V.munitZio % Test)

}

publish / skip := true

// #########################
// ### Models & Services ###
// #########################

/** Just data models and interfaces of service.
  *
  * This module must not depend on external libraries!
  */
lazy val models = project
  .in(file("mercury/mercury-library/models"))
  .settings(name := "mercury-data-models")
  .settings(
    libraryDependencies ++= Seq(D.zio.value),
    libraryDependencies ++= Seq(
      D.circeCore.value,
      D.circeGeneric.value,
      D.circeParser.value
    ), // TODO try to remove this from this module
    // libraryDependencies += D.didScala.value
  )
  .settings(libraryDependencies += D.jwk.value) //FIXME just for the DidAgent

/* TODO move code from agentDidcommx to here
models implementation for didcommx () */
// lazy val modelsDidcommx = project
//   .in(file("models-didcommx"))
//   .settings(name := "mercury-models-didcommx")
//   .settings(libraryDependencies += D.didcommx.value)
//   .dependsOn(models)

// #################
// ### Protocols ###
// #################

lazy val protocolConnection = project
  .in(file("mercury/mercury-library/protocol-connection"))
  .settings(name := "mercury-protocol-connection")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(models, protocolInvitation)

lazy val protocolCoordinateMediation = project
  .in(file("mercury/mercury-library/protocol-coordinate-mediation"))
  .settings(name := "mercury-protocol-coordinate-mediation")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(models)

lazy val protocolDidExchange = project
  .in(file("mercury/mercury-library/protocol-did-exchange"))
  .settings(name := "mercury-protocol-did-exchange")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .dependsOn(models, protocolInvitation)

lazy val protocolInvitation = project
  .in(file("mercury/mercury-library/protocol-invitation"))
  .settings(name := "mercury-protocol-invitation")
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

lazy val protocolMercuryMailbox = project
  .in(file("mercury/mercury-library/protocol-mercury-mailbox"))
  .settings(name := "mercury-protocol-mailbox")
  .settings(libraryDependencies += D.zio.value)
  .dependsOn(models, protocolInvitation, protocolRouting)

lazy val protocolLogin = project
  .in(file("mercury/mercury-library/protocol-outofband-login"))
  .settings(name := "mercury-protocol-outofband-login")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(models)

lazy val protocolReportProblem = project
  .in(file("mercury/mercury-library/protocol-report-problem"))
  .settings(name := "mercury-protocol-report-problem")
  .dependsOn(models)

lazy val protocolRouting = project
  .in(file("mercury/mercury-library/protocol-routing"))
  .settings(name := "mercury-protocol-routing-2-0")
  .settings(libraryDependencies += D.zio.value)
  .dependsOn(models)

lazy val protocolIssueCredential = project
  .in(file("mercury/mercury-library/protocol-issue-credential"))
  .settings(name := "mercury-protocol-issue-credential")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(models)

lazy val protocolPresentProof = project
  .in(file("mercury/mercury-library/protocol-present-proof"))
  .settings(name := "mercury-protocol-present-proof")
  .settings(libraryDependencies += D.zio.value)
  .settings(libraryDependencies ++= Seq(D.circeCore.value, D.circeGeneric.value, D.circeParser.value))
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(models)

// ################
// ### Resolver ###
// ################

// TODO move stuff to the models module
lazy val resolver = project // maybe merge into models
  .in(file("mercury/mercury-library/resolver"))
  .settings(name := "mercury-resolver")
  .settings(
    libraryDependencies ++= Seq(
      D.didcommx.value,
      D.peerDidcommx.value,
      D.munit.value,
      D.munitZio.value,
      D.jwk.value,
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(models)

// ##############
// ### Agents ###
// ##############

lazy val agent = project // maybe merge into models
  .in(file("mercury/mercury-library/agent"))
  .settings(name := "mercury-agent-core")
  .settings(libraryDependencies ++= Seq(D.zioLog.value)) // , D.zioSLF4J.value))
  .dependsOn(
    models,
    resolver,
    protocolCoordinateMediation,
    protocolInvitation,
    protocolRouting,
    protocolMercuryMailbox,
    protocolLogin,
    protocolIssueCredential,
    protocolPresentProof,
    protocolConnection,
  )

/** agents implementation with didcommx */
lazy val agentDidcommx = project
  .in(file("mercury/mercury-library/agent-didcommx"))
  .settings(name := "mercury-agent-didcommx")
  .settings(libraryDependencies += D.didcommx.value)
  .settings(libraryDependencies += D.munitZio.value)
  .dependsOn(agent) //modelsDidcommx

/** Demos agents and services implementation with didcommx */
lazy val agentCliDidcommx = project
  .in(file("mercury/mercury-library/agent-cli-didcommx"))
  .settings(name := "mercury-agent-cli-didcommx")
  .settings(libraryDependencies += "com.google.zxing" % "core" % "3.5.0")
  .settings(libraryDependencies += D.zioHttp.value)
  .dependsOn(agentDidcommx)

// ///** TODO Demos agents and services implementation with did-scala */
// lazy val agentDidScala =
//   project
//     .in(file("mercury/mercury-library/agent-did-scala"))
//     .settings(name := "mercury-agent-didscala")
//     .settings(skip / publish := true)
//     .dependsOn(agent)

// ### Test coverage ###
sys.env
  .get("SBT_SCOVERAGE")
  .map { _ =>
    lazy val coverageDataDir: SettingKey[File] =
      settingKey[File]("directory where the measurements and report files will be stored")
    coverageDataDir := target.value / "coverage"
  }
  .toSeq

// ### ReleaseStep ###
sys.env
  .get("SBT_PACKAGER") // SEE also plugin.sbt
  .map { _ =>
    println("### Config SBT_PACKAGER (releaseProcess) ###")
  import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    publishArtifacts,
    setNextVersion
  )
  }
  .toSeq

// #####################
// #####  pollux  ######
// #####################

val polluxCommonSettings = Seq(
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
  resolvers += Resolver.githubPackages("input-output-hk"),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven"
)

lazy val polluxVcJWT = project
  .in(file("pollux/lib/vc-jwt"))
  .settings(polluxCommonSettings)
  .settings(
    name := "pollux-vc-jwt",
    libraryDependencies ++= PolluxDependencies_VC_JWT.polluxVcJwtDependencies
  )

lazy val polluxCore = project
  .in(file("pollux/lib/core"))
  .settings(polluxCommonSettings)
  .settings(
    name := "pollux-core",
    libraryDependencies ++= PolluxDependencies.coreDependencies
  )
  .dependsOn(polluxVcJWT)
  .dependsOn(protocolIssueCredential, protocolPresentProof, resolver)

lazy val polluxDoobie = project
  .in(file("pollux/lib/sql-doobie"))
  .settings(polluxCommonSettings)
  .settings(
    name := "pollux-sql-doobie",
    libraryDependencies ++= PolluxDependencies.sqlDoobieDependencies
  )
  .dependsOn(polluxCore % "compile->compile;test->test")

// #####################
// #####  connect  #####
// #####################

def connectCommonSettings = polluxCommonSettings

lazy val connectCore = project
  .in(file("connect/lib/core"))
  .settings(connectCommonSettings)
  .settings(
    name := "connect-core",
    libraryDependencies ++= ConnectDependencies.coreDependencies,
    Test / publishArtifact := true
  )
  .dependsOn(protocolConnection, protocolReportProblem)

lazy val connectDoobie = project
  .in(file("connect/lib/sql-doobie"))
  .settings(connectCommonSettings)
  .settings(
    name := "connect-sql-doobie",
    libraryDependencies ++= ConnectDependencies.sqlDoobieDependencies
  )
  .dependsOn(connectCore % "compile->compile;test->test")

// #####################
// #### Prism Agent ####
// #####################
import sbtghpackages.GitHubPackagesPlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

def prismAgentConnectCommonSettings = polluxCommonSettings

// val apiBaseDirectory =
//   settingKey[File]("The base directory for PrismAgent API specifications")
// inThisBuild(
//   Seq(
//     apiBaseDirectory := baseDirectory.value / "api"
//   )
// )

lazy val prismAgentWalletAPI = project
  .in(file("prism-agent/service/wallet-api"))
  .settings(prismAgentConnectCommonSettings)
  .settings(
    name := "prism-agent-wallet-api",
    libraryDependencies ++= PrismAgentDependencies.keyManagementDependencies
  )
  .dependsOn(agentDidcommx)

// lazy val prismAgentServer = project
//   .in(file("prism-agent/service/server"))
//   .settings(prismAgentConnectCommonSettings)
//   .settings(
//     name := "prism-agent",
//     fork := true,
//     libraryDependencies ++= PrismAgentDependencies.serverDependencies,
//     Compile / mainClass := Some("io.iohk.atala.agent.server.Main"),
//     // OpenAPI settings
//     Compile / unmanagedResourceDirectories += baseDirectory.value / ".." / "api",
//     Compile / sourceGenerators += openApiGenerateClasses,
//     openApiGeneratorSpec := baseDirectory.value / ".." / "api" / "http/prism-agent-openapi-spec.yaml",
//     openApiGeneratorConfig := baseDirectory.value / "openapi/generator-config/config2.yaml",
//     openApiGeneratorImportMapping := Seq(
//       "DidOperationType",
//       "DidOperationStatus"
//     )
//       .map(model => (model, s"io.iohk.atala.agent.server.http.model.OASModelPatches.$model"))
//       .toMap,
//     // FIXME
//     // Docker / maintainer := "atala-coredid@iohk.io",
//     // Docker / dockerUsername := Some("input-output-hk"),
//     // Docker / githubOwner := "atala-prism-building-blocks",
//     // Docker / dockerRepository := Some("ghcr.io"),
//     // dockerExposedPorts := Seq(8080, 8085, 8090),
//     // dockerBaseImage := "openjdk:11"
//   )
//   // FIXME .enablePlugins(OpenApiGeneratorPlugin, JavaAppPackaging, DockerPlugin)
//   .enablePlugins(OpenApiGeneratorPlugin)
//   .dependsOn(prismAgentWalletAPI)
//   .dependsOn(agent, polluxCore, polluxDoobie, connectCore, connectDoobie)
