import sbt._

object Dependencies {

  val log4j2Version = "2.10.0"
  val scalaTestVersion = "3.0.3"
  val kafkaApiVersion = "1.0.0"
  val servingGrpcScala = "0.0.13"

  lazy val logDependencies = Seq(
    "org.apache.logging.log4j" % "log4j-api" % log4j2Version,
    "org.apache.logging.log4j" % "log4j-core" % log4j2Version,
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j2Version,
    "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0"
  )

  lazy val hydroserving = Seq(
    "io.hydrosphere" %% "serving-grpc-scala" % servingGrpcScala,
    "io.hydrosphere" %% "envoy-data-plane-api" % "v1.5.0_2"
  ).map(_.exclude("org.slf4j", "slf4j-jdk14"))


  lazy val commonDependencies = logDependencies
    .union(Seq(
      "com.typesafe" % "config" % "1.3.2",
      "org.typelevel" %% "cats-core" % "1.0.1"
    ))

  lazy val dockerDependencies = Seq(
    "com.whisk" %% "docker-testkit-scalatest" % "0.9.5" % Test,
    "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.5" % Test,
    "com.spotify" % "docker-client" % "8.10.0"
  ).map(_.exclude("org.slf4j", "slf4j-jdk14"))


  lazy val streamingKafkaDependencies =
    dockerDependencies
    .union(hydroserving)
    .union(Seq(
      "org.apache.kafka" %% "kafka" % kafkaApiVersion,
      "org.apache.kafka" % "kafka-clients" % kafkaApiVersion % Test,
      "org.apache.kafka" % "kafka-streams" % kafkaApiVersion,
      "com.fasterxml.jackson.core" % "jackson-core" % "2.9.2",
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
    )).map(_.exclude("org.slf4j", "slf4j-jdk14"))
    .union(commonDependencies)




}
