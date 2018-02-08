package io.hydrosphere.serving.kafka.it

import io.grpc.ManagedChannelBuilder
import io.hydrosphere.serving.kafka.Flow
import io.hydrosphere.serving.kafka.config._
import io.hydrosphere.serving.kafka.it.infrostructure.{FakeModel, KafkaContainer, TestProducer}
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.mappers.KafkaServingMessageSerde
import io.hydrosphere.serving.kafka.predict.{Application, ApplicationService, PredictService, PredictServiceImpl}
import io.hydrosphere.serving.kafka.stream.KafkaStreamer
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionStage, KafkaStreaming}
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import org.apache.kafka.common.serialization.Serdes
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers, Suite}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class AppSpec extends FlatSpec
  with KafkaContainer
  with Matchers
  with BeforeAndAfter
   { self: Suite =>

  var fakeModelServer:FakeModel = null

  before {
    createTopic("success")
    createTopic("failure")
    createTopic("test")
    fakeModelServer = Await.result(FakeModel.runAsync(56787), 10 seconds)
  }

  after {
    fakeModelServer.stop()
    deleteTopics()
  }

  "App" should "Read valuesf from kafka, save predicted values" in {

    implicit val config = Configuration(
      ApplicationConfig(56789, "localhost"),
      SidecarConfig("localhost", 56788),
      KafkaConfiguration("localhost", 9092)
    )

    implicit val appService = new ApplicationService {
      override def getApplications(): Seq[Application] = Seq(
        Application("test-app",
          Some("test"),
          Some("success"),
          Some("failure"),
          ModelSpec(),
          ExecutionGraph(Seq(ExecutionStage())))
      )
    }

    implicit val streamer = new KafkaStreamer[Array[Byte], KafkaServingMessage](Serdes.ByteArray().getClass, classOf[KafkaServingMessageSerde])
    implicit val modelChanel = ManagedChannelBuilder.forAddress("localhost", 56786).usePlaintext(true).build

    implicit val predictService: PredictService = new PredictServiceImpl

    new Thread("flow-thread"){
      override def run(): Unit = {
        Flow.start(AppContext())
      }
    }.start()

    new TestProducer()


  }

}
