
name := "hydro-serving-kafka"

val scalaCommonVersion = "2.11.11"

scalaVersion := scalaCommonVersion

lazy val currentAppVersion = util.Properties.propOrElse("appVersion", "0.0.1")
version := currentAppVersion
parallelExecution in Test := false
parallelExecution in IntegrationTest := false
fork in(Test, test) := true
fork in(IntegrationTest, test) := true
fork in(IntegrationTest, testOnly) := true
publishArtifact := false
organization := "io.hydrosphere.serving.kafka"
scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps"
)

exportJars := true
resolvers += Resolver.bintrayRepo("findify", "maven")
libraryDependencies ++= Dependencies.streamingKafkaDependencies


