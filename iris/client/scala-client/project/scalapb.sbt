addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")

libraryDependencies ++= Seq("com.thesamet.scalapb" %% "compilerplugin" % "0.11.11")
