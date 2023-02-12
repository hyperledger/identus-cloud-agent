addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")

// USE> GITHUB_TOKEN=??? SBT_PACKAGER=enable sbt publish
new sbt.Def.SettingList(
  sys.env
    .get("SBT_PACKAGER")
    .map { _ =>
      println("### Enable sbt-native-packager ###")
      addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.14")
    }
    .toSeq
)

// USE> SBT_SCOVERAGE=enable sbt clean coverage test coverageAggregate
new sbt.Def.SettingList(
  sys.env
    .get("SBT_SCOVERAGE") // SEE also build.sbt
    .map { _ =>
      println("### Enable sbt-scoverage ###")
      addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.7") // Needs scala version 3.2.2
    }
    .toSeq
)
