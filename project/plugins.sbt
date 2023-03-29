// addSbtPlugin("org.jetbrains.scala" % "sbt-ide-settings" % "1.1.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")
// addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.9")

// In order to import proper version of com.google.protobuf.ByteString we need to add this dependency
libraryDependencies ++= Seq("com.thesamet.scalapb" %% "compilerplugin" % "0.11.12")

libraryDependencies ++= Seq("org.openapitools" % "openapi-generator" % "6.0.1")

// USE> GITHUB_TOKEN=??? SBT_PACKAGER=enable sbt publish
new sbt.Def.SettingList(
  sys.env
    .get("SBT_PACKAGER")
    .map { _ =>
      println("### Enable sbt-native-packager ###")
      addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.11")
    }
    .toSeq
)

// USE> SBT_SCOVERAGE=enable sbt clean coverage test coverageAggregate
new sbt.Def.SettingList(
  sys.env
    .get("SBT_SCOVERAGE") // SEE also build.sbt
    .map { _ =>
      println("### Enable sbt-scoverage ###")
      addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.6") // Needs scala version 3.2.2
    }
    .toSeq
)
