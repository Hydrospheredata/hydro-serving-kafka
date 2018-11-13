import sbt._

name := "hydro-serving-kafka"

enablePlugins(DockerPlugin)

scalaVersion := "2.12.4"

version := IO.read(file("version"))
parallelExecution in Test := false
parallelExecution in IntegrationTest := false
fork in(Test, test) := true
fork in(IntegrationTest, test) := true
fork in(IntegrationTest, testOnly) := true
publishArtifact := false
organization := "io.hydrosphere"
scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps"
)

libraryDependencies ++= Dependencies.all

dockerfile in docker := {
  val dockerFilesLocation = baseDirectory.value / "src/main/docker/"
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (dependencyClasspath in Compile).value
  val artifactTargetPath = s"/app/app.jar"

  new Dockerfile {
    from("openjdk:8u151-jre-alpine")

    env("SIDECAR_INGRESS_PORT", "8080")
    env("SIDECAR_EGRESS_PORT", "8081")
    env("SIDECAR_ADMIN_PORT", "8082")
    env("SIDECAR_HOST", "sidecar")
    env("KAFKA_HOST","kafka")
    env("KAFKA_PORT","9092")
    env("APP_PORT", "9091")
    env("APP_ID", "hydro-serving-kafka")

    label("SERVICE_ID", "-12")
    label("HS_SERVICE_MARKER", "HS_SERVICE_MARKER")
    label("DEPLOYMENT_TYPE", "APP")
    label("RUNTIME_ID", "-12")
    label("SERVICE_NAME", "gateway-kafka")

    add(dockerFilesLocation, "/app/")
    add(classpath.files, "/app/lib/")
    add(jarFile, artifactTargetPath)

    cmd("/app/start.sh")
  }
}

imageNames in docker := Seq(
  ImageName(
    namespace = Some("hydrosphere"),
    repository = s"serving-gateway-kafka"
  )
)
