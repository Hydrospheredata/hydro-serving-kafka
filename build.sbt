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
    repository = s"serving-kafka-gateway"
  )
)
