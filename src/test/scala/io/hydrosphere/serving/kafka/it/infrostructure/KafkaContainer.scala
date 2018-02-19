package io.hydrosphere.serving.kafka.it.infrostructure

import java.nio.file.{Path, Paths}
import java.util.concurrent.TimeUnit

import com.spotify.docker.client.DockerClient.ExecStartParameter
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.{DockerContainer, DockerFactory, DockerKit, DockerReadyChecker}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.io.{File, Path}

trait KafkaContainer extends ScalaFutures
  with BeforeAndAfterAll
  with DockerKit {
  self: Suite =>

  private val client: DockerClient = DefaultDockerClient.fromEnv().build()
  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(client)
  val log: Logger = LoggerFactory.getLogger(classOf[KafkaContainer])

  def dockerInitPatienceInterval =
    PatienceConfig(scaled(Span(20, Seconds)), scaled(Span(10, Millis)))

  def dockerPullImagesPatienceInterval =
    PatienceConfig(scaled(Span(1200, Seconds)), scaled(Span(250, Millis)))


  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startAllOrFail()
    allowDeletion()
    restartKafka()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopAllQuietly()
  }

  private[this] val path = "/opt/kafka_2.11-0.10.1.0/bin"
  private[this] val topicScript = s"$path/kafka-topics.sh"
  private[this] val zookeeperParam = "--zookeeper localhost:2181"

  def createTopic(topicName: String, partitions: Int = 1, replication: Int = 1): Unit = {
    val cmd = s"$topicScript --create $zookeeperParam " +
      s"--replication-factor $replication --partitions $partitions --topic $topicName"
    exec(containerName(), cmd)
  }

  def deleteTopics(): Unit = for (
    topic <- topicsList() if !topic.startsWith("_")) {
    topicDelete(topic)
  }


  def topicsList(): Seq[String] = {
    val logMessage = exec(containerName(), s"$topicScript --list $zookeeperParam")
    logMessage.split("\n")
  }

  def topicExists(topicName: String): Boolean = {
    val cmd = s"$topicScript --describe --topic $topicName $zookeeperParam"
    val result = exec(containerName(), cmd)
    result != null && !result.isEmpty
  }

  def topicDelete(topicName: String): Unit = {
    val cmd = s"$topicScript --delete --topic $topicName $zookeeperParam"
    exec(containerName(), cmd)
  }

    def allowDeletion(): Unit = {
      val cmd = "echo \"delete.topic.enable=true\" >> /opt/kafka_2.11-0.10.1.0/config/server.properties"
      log.info("setting delete.topic.enable=true")
      exec(containerName(), cmd)
    }

    def restartKafka(): Unit = {
      stopKafka()
      //stopZookeeper()
      //startZookeeper()
      startKafka()
    }

  def stopKafka(): Unit = {
    val cmd = s"$path/kafka-server-stop.sh"
    log.info("stopping kafka")
    exec(containerName(), cmd)
    TimeUnit.SECONDS.sleep(2)
  }

  def startKafka(): Unit = {
    val cmd = s" $path/kafka-server-start.sh /opt/kafka_2.11-0.10.1.0/config/server.properties"
    log.info("starting kafka")
    exec(containerName(), cmd)
    TimeUnit.SECONDS.sleep(2)
  }

  def stopZookeeper(): Unit = {
    val cmd = s"$path/zookeeper-server-stop.sh"
    log.info("stopping zookeeper")
    exec(containerName(), cmd)
  }

  def startZookeeper(): Unit = {
    val cmd = s"$path/zookeeper-server-start.sh /opt/kafka_2.11-0.10.1.0/config/zookeeper.properties"
    log.info("starting zookeeper")
    exec(containerName(), cmd)
  }

  def containerName(): String = Await.result(containerManager.getContainerState(kafkaContainer).getName(), 2 second)

  private[KafkaContainer] def exec(dockerName: String, cmd: String): String = {
    val arr = Array("sh", "-c", cmd)
    val execCreation = client.execCreate(dockerName.replace("/", ""), arr, DockerClient.ExecCreateParam.attachStdin(),
      DockerClient.ExecCreateParam.attachStdout())
    val logMessage = client.execStart(execCreation.id, ExecStartParameter.TTY).readFully()
    log.info(logMessage)
    logMessage
  }

  def KafkaAdvertisedPort = 9092

  val zookeeperDefaultPort = 2181


  lazy val kafkaContainer: DockerContainer = DockerContainer("spotify/kafka")
    .withPorts(
      KafkaAdvertisedPort -> Some(9092),
      zookeeperDefaultPort -> Some(zookeeperDefaultPort)
    )
    .withEnv(s"ADVERTISED_PORT=$KafkaAdvertisedPort", s"ADVERTISED_HOST=${dockerExecutor.host}")
    .withReadyChecker(DockerReadyChecker.LogLineContains("kafka entered RUNNING state"))

  abstract override def dockerContainers: List[DockerContainer] =
    kafkaContainer :: super.dockerContainers

}


