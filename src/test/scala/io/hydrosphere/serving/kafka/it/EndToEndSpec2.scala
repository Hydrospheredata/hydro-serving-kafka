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
//class EndToEndSpec2 extends FlatSpec
//  with Matchers
//  with BeforeAndAfter {
//  self: Suite =>
//
//  var testConsumer: TestConsumer[Array[Byte], KafkaServingMessage] = _
//  var testProducer: TestProducer[Integer, KafkaServingMessage] = _
//
//  before {
//    testConsumer = new TestConsumer[Array[Byte], KafkaServingMessage]("localhost:19092", "test_11", Serdes.ByteArray().getClass,
//      classOf[KafkaServingMessageSerde])
//
//    testProducer = new TestProducer[Integer, KafkaServingMessage](
//      hostAndPort = "localhost:19092",
//      keySerializer = classOf[IntegerSerializer],
//      valSerializer = classOf[KafkaServingMessageSerializer])
//  }
//
//  after {
//    testConsumer.close()
//    testProducer.close()
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
//    Range(0, 10).foreach { i =>
//      testProducer.send(i, message(i))
//    }
//
//    TimeUnit.SECONDS sleep 5
//
//    testConsumer.out.size shouldBe 10
//
//    println(s"in size: ${testConsumer.in.size}")
//    println(s"out size: ${testConsumer.out.size}")
//    println(s"failure size: ${testConsumer.failure.size}")
//    println(testConsumer.out)
//
//
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
