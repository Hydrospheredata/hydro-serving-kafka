package io.hydrosphere.serving.kafka.it

import java.util.concurrent.TimeUnit

import io.grpc.{ClientInterceptors, ManagedChannel, ManagedChannelBuilder}
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.grpc.{AuthorityReplacerInterceptor, Headers}
import io.hydrosphere.serving.kafka.Flow
import io.hydrosphere.serving.kafka.config.Inject.rpcChanel
import io.hydrosphere.serving.kafka.it.infrostructure.{FakeModel, KafkaContainer, TestConsumer}
import io.hydrosphere.serving.kafka.kafka_messages.{KafkaMessageMeta, KafkaServingMessage}
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage.RequestOrError
import io.hydrosphere.serving.kafka.mappers.{KafkaServingMessageSerde, KafkaServingMessageSerializer}
import io.hydrosphere.serving.manager.grpc.applications.ExecutionStage
import io.hydrosphere.serving.tensorflow.api.predict.PredictRequest
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.apache.kafka.common.serialization._
import org.scalatest._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import io.hydrosphere.serving.kafka.stream.Producer
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc

class AppSpec extends FlatSpec
  with KafkaContainer
  with GivenWhenThen
  with Matchers
  with BeforeAndAfter {
  self: Suite =>

  var fakeModelServer: FakeModel = _
  var testConsumer: TestConsumer[Array[Byte], KafkaServingMessage] = _
  var testProducer: Producer[Integer, KafkaServingMessage] = _

  before {
    createTopic("success")
    createTopic("failure")
    createTopic("shadow_topic")
    createTopic("test")
    fakeModelServer = Await.result(FakeModel.runAsync(56787, 56786), 10 seconds)
    testConsumer = new TestConsumer[Array[Byte], KafkaServingMessage]("localhost:9092", "test_1", Serdes.ByteArray().getClass,
      classOf[KafkaServingMessageSerde])

    testProducer = new Producer[Integer, KafkaServingMessage](
      hostAndPort = "localhost:9092",
      keySerializer = classOf[IntegerSerializer],
      valSerializer = classOf[KafkaServingMessageSerializer])
  }

  after {
    fakeModelServer.stop()
    testConsumer.close()
    testProducer.close()
    deleteTopics()
  }

  def executionGraph(num: Int): ExecutionStage = ExecutionStage(
    stageId = s"stage$num",
    signature = Some(ModelSignature(
      signatureName = s"Signature$num"
    ))
  )

  "Application" should "accept kafka messages" in {

    withApplication { _ =>

      val rpcChanel: ManagedChannel = ManagedChannelBuilder
        .forAddress("localhost", 56789)
        .usePlaintext(true)
        .build
      val stub = PredictionServiceGrpc.stub(ClientInterceptors
        .intercept(rpcChanel, new AuthorityReplacerInterceptor, Headers.KafkaTopic.interceptor))

      TimeUnit.SECONDS.sleep(2)

      And("valid messages with request been published via kafka")
      Range(0, 10).foreach { i =>
        testProducer.send("test", i, message(i))
      }


      And("valid test messages been published via grpc")
      Range(0, 10).foreach { i =>
        val result = stub
          .withOption(Headers.KafkaTopic.callOptionsKey, "shadow_topic")
          .predict(message(i).getRequest.withModelSpec(
            ModelSpec(
              name = "someApp"
            )
          ))

        val responce = Await.result(result, 10 second)
      }

    }

    Then("All result predictions should be processed and published to 'successful' topic")
    testConsumer.out.size shouldBe 10
    testConsumer.out.filter(_.requestOrError.isRequest).size shouldBe 10

    And("inner model stage computation results should be published to 'shadow' topic")
    testConsumer.shadow.size shouldBe 30
    testConsumer.shadow.filter(_.requestOrError.isRequest).size shouldBe 30
  }

  def withApplication(action: Flow => Unit): Unit = {
    When("test Application been started")
    import io.hydrosphere.serving.kafka.it.infrostructure.TestInject._
    var flow: Flow = new Flow()
    try {
      Future {
        flow.start()
      }

      action(flow)

      TimeUnit.SECONDS sleep 5
    } finally {
      flow.stop()
    }
  }


  def message(num: Int): KafkaServingMessage = {

    val proto = TensorProto(dtype = DataType.DT_DOUBLE, doubleVal = Seq(num), versionNumber = num)
    val req = PredictRequest(inputs = Map("VeryImportantKey" -> proto))

    KafkaServingMessage(
      meta = Some(KafkaMessageMeta().withTraceId("traceId")),
      requestOrError = RequestOrError.Request(req)
    )

  }

}

