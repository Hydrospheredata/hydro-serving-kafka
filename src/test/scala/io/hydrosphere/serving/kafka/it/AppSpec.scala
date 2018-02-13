package io.hydrosphere.serving.kafka.it

import java.util.concurrent.TimeUnit

import io.grpc.ManagedChannelBuilder
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.kafka.Flow
import io.hydrosphere.serving.kafka.config._
import io.hydrosphere.serving.kafka.it.infrostructure.{FakeModel, KafkaContainer, TestConsumer, TestProducer}
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage.RequestOrError
import io.hydrosphere.serving.kafka.mappers.{KafkaServingMessageDeserializer, KafkaServingMessageSerde, KafkaServingMessageSerializer}
import io.hydrosphere.serving.kafka.predict.{Application, PredictService, PredictServiceImpl, XDSApplicationUpdateService}
import io.hydrosphere.serving.kafka.stream.KafkaStreamer
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionStage, KafkaStreaming}
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.PredictRequest
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.apache.kafka.common.serialization._
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
    fakeModelServer = Await.result(FakeModel.runAsync(56787, 56786), 10 seconds)
  }

  after {
    fakeModelServer.stop()
    deleteTopics()
  }

     def executionGraph(num:Int) = (ExecutionStage(
       stageId = s"stage$num",
       signature = Some(ModelSignature(
         signatureName = s"Signature$num"
       ))
     ))

  "App" should "Read valuesf from kafka, save predicted values" in {

    implicit val config = Configuration(
      ApplicationConfig(56789, "localhost"),
      SidecarConfig("localhost", 56788),
      KafkaConfiguration("localhost", 9092)
    )


    implicit val streamer = new KafkaStreamer[Array[Byte], KafkaServingMessage](Serdes.ByteArray().getClass, classOf[KafkaServingMessageSerde])
    implicit val modelChanel = ManagedChannelBuilder.forAddress("localhost", 56787).usePlaintext(true).build
    val appChanel = ManagedChannelBuilder.forAddress("localhost", 56786).usePlaintext(true).build


    implicit val predictService: PredictService = new PredictServiceImpl

    implicit val applicationUpdater = new XDSApplicationUpdateService()(appChanel)

    val consumer = new TestConsumer[Array[Byte], KafkaServingMessage]("localhost:9092", "test_1", Serdes.ByteArray().getClass,
      classOf[KafkaServingMessageSerde])

    val context = AppContext();

    new Thread("flow-thread"){
      override def run(): Unit = {
        Flow.start(context)
      }
    }.start()

    val producer = new TestProducer[Integer, KafkaServingMessage](
      keySerializer = classOf[IntegerSerializer],
      valSerializer = classOf[KafkaServingMessageSerializer])

    Range(0, 10).foreach { i =>
      producer.send(i, message(i))
    }

    TimeUnit.SECONDS.sleep(5)

    consumer.out.size shouldBe(10)

    Flow.stop(context)
  }



  def message(num: Int): KafkaServingMessage = {

    val proto = TensorProto(dtype = DataType.DT_DOUBLE, doubleVal = Seq(num), versionNumber = num)
    val req = PredictRequest(inputs = Map("VeryImportantnKey" -> proto))

    KafkaServingMessage(traceId = s"traceId_$num", requestOrError = RequestOrError.Request(req))

  }

}
