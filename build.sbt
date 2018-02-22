import sbt._

name := "hydro-serving-kafka"

val scalaCommonVersion = "2.12.4"

enablePlugins(DockerPlugin)

scalaVersion := scalaCommonVersion

version := IO.read(file("version"))
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

dockerfile in docker := {
  val dockerFilesLocation = baseDirectory.value / "src/main/docker/"
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (dependencyClasspath in Compile).value
  val artifactTargetPath = s"/app/app.jar"

  new Dockerfile {
    from("anapsix/alpine-java:8")

    env("SIDECAR_INGRESS_PORT", "8080")
    env("SIDECAR_EGRESS_PORT", "8081")
    env("SIDECAR_ADMIN_PORT", "8082")
    env("SIDECAR_HOST", "localhost")
    env("KAFKA_HOST","localhost")
    env("KAFKA_PORT","9092")
    env("APP_PORT", "9060")
    env("APP_ID", "hydro-serving-kafka")

    label("DEPLOYMENT_TYPE", "APP")

    add(dockerFilesLocation, "/app/")
    add(classpath.files, "/app/lib/")
    add(jarFile, artifactTargetPath)

   // volume("/model")

    cmd("/app/start.sh")
  }
}

imageNames in docker := Seq(
  ImageName(
    namespace = Some("hydrosphere"),
    repository = s"serving-kafka-gateway"
  )
)
