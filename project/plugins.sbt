logLevel := Level.Info

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

libraryDependencies ++= Seq(
  "com.spotify" % "docker-client" % "8.8.0"
)


