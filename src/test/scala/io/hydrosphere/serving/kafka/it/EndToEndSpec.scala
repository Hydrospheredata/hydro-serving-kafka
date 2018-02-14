//package io.hydrosphere.serving.kafka.it
//
//import java.util.concurrent.TimeUnit
//
//import io.hydrosphere.serving.contract.model_signature.ModelSignature
//import io.hydrosphere.serving.kafka.Flow
//import io.hydrosphere.serving.kafka.it.infrostructure.{FakeModel, KafkaContainer, TestConsumer, TestProducer}
//import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
//import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage.RequestOrError
//import io.hydrosphere.serving.kafka.mappers.{KafkaServingMessageSerde, KafkaServingMessageSerializer}
//import io.hydrosphere.serving.manager.grpc.applications.ExecutionStage
//import io.hydrosphere.serving.tensorflow.api.predict.PredictRequest
//import io.hydrosphere.serving.tensorflow.tensor.TensorProto
//import io.hydrosphere.serving.tensorflow.types.DataType
//import org.apache.kafka.common.serialization.{IntegerSerializer, Serdes}
//import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers, Suite}
//
//import scala.concurrent.{Await, Future}
//
//
//class EndToEndSpec extends FlatSpec
//  with KafkaContainer
//  with Matchers
//  with BeforeAndAfter {
//  self: Suite =>
//
//  import io.hydrosphere.serving.kafka.it.infrostructure.EndToEndInject._
//
//  var testConsumer: TestConsumer[Array[Byte], KafkaServingMessage] = _
//  var testProducer: TestProducer[Integer, KafkaServingMessage] = _
//
//  before {
//    createTopic("success1")
//    createTopic("failure1")
//    createTopic("test1")
//
//    testConsumer = new TestConsumer[Array[Byte], KafkaServingMessage]("localhost:9092", "test_1", Serdes.ByteArray().getClass,
//      classOf[KafkaServingMessageSerde])
//
//    testProducer = new TestProducer[Integer, KafkaServingMessage](
//      keySerializer = classOf[IntegerSerializer],
//      valSerializer = classOf[KafkaServingMessageSerializer])
//  }
//
//  after {
//    testConsumer.close()
//    testProducer.close()
//    deleteTopics()
//  }
//
//  def executionGraph(num: Int): ExecutionStage = ExecutionStage(
//    stageId = s"stage$num",
//    signature = Some(ModelSignature(
//      signatureName = s"Signature$num"
//    ))
//  )
//
//  "App" should "Read values from kafka, save predicted values" in {
//
//    var flow: Flow = new Flow()
//
//    Future {
//      flow.start()
//    }
//
//    Range(0, 10).foreach { i =>
//      testProducer.send(i, message(i))
//    }
//
//    TimeUnit.SECONDS sleep 5
//
//    testConsumer.out.size shouldBe 10
//
//    flow.stop()
//  }
//
//
//  def message(num: Int): KafkaServingMessage = {
//
//    val proto = TensorProto(dtype = DataType.DT_DOUBLE, doubleVal = Seq(num), versionNumber = num)
//    val req = PredictRequest(inputs = Map("VeryImportantKey" -> proto))
//
//    KafkaServingMessage(traceId = s"traceId_$num", requestOrError = RequestOrError.Request(req))
//
//  }
//
//}
