addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.7")

// Add publishing only conditionally if `SBT_PACKAGER`
// USE> GITHUB_TOKEN=??? SBT_PACKAGER=enable sbt publish
new sbt.Def.SettingList(
  sys.env
    .get("SBT_PACKAGER")
    .map { _ =>
      println("### Enable sbt-native-packager ###")
      addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
      addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")
      addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
    }
    .toSeq
)
