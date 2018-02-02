package io.hydrosphere.serving.kafka.it

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.{time, _}
import org.scalatest.time._
import scala.concurrent.duration._

import scala.concurrent.Await

class KafkaSpec extends FlatSpec
  with Matchers
  with KafkaContainer
  with BeforeAndAfter {

  "kafka container" should "topic should be created" in {
    val topicName = "test"
    topicExists(topicName) shouldBe false
    createTopic(topicName)
    topicExists(topicName) shouldBe true
  }

  "kafka container" should "topic should be deleted" in {
    val topicName = "test"
    topicExists(topicName) shouldBe false
    createTopic(topicName)
    topicExists(topicName) shouldBe true
  }
}
