logLevel := Level.Info

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.9.0")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

libraryDependencies ++= Seq(
  "com.spotify" % "docker-client" % "8.8.0"
)


