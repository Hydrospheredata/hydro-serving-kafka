//package io.hydrosphere.serving.kafka.it
//
//import java.util.concurrent.TimeUnit
//
//import com.google.protobuf.ByteString
//import io.hydrosphere.serving.contract.model_signature.ModelSignature
//import io.hydrosphere.serving.kafka.Flow
//import io.hydrosphere.serving.kafka.it.infrostructure.{FakeModel, KafkaContainer, TestConsumer}
//import io.hydrosphere.serving.kafka.kafka_messages.{KafkaMessageMeta, KafkaServingMessage}
//import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage.RequestOrError
//import io.hydrosphere.serving.kafka.mappers.{KafkaServingMessageSerde, KafkaServingMessageSerializer}
//import io.hydrosphere.serving.kafka.stream.Producer
//import io.hydrosphere.serving.manager.grpc.applications.ExecutionStage
//import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
//import io.hydrosphere.serving.tensorflow.api.predict.PredictRequest
//import io.hydrosphere.serving.tensorflow.tensor.TensorProto
//import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
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
//  var testProducer: Producer[Integer, KafkaServingMessage] = _
//
//  before {
//    testConsumer = new TestConsumer[Array[Byte], KafkaServingMessage]("localhost:19092", "test_11", Serdes.ByteArray().getClass,
//      classOf[KafkaServingMessageSerde])
//
//    testProducer = new Producer[Integer, KafkaServingMessage](
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
//    Range(0, 1).foreach { i =>
//      testProducer.send("test", i, message(i))
//    }
//
//    TimeUnit.SECONDS sleep 5
//
//    testConsumer.out.size shouldBe 10
//    testConsumer.shadow.size shouldBe 30
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
//    val newtxt = Seq(txt, "class", "Java")
//    val proto = TensorProto(
//      dtype = DataType.DT_STRING,
//      stringVal = newtxt.map(ByteString.copyFromUtf8(_)),
//      versionNumber = num,
//      tensorShape = Some(
//        TensorShapeProto(
//          dim = Seq(
//            TensorShapeProto.Dim(-1)
//          )
//        )
//      )
//    )
//    val req = PredictRequest(
//      inputs = Map("text" -> proto),
//      modelSpec = Some(ModelSpec(
//        signatureName = "default_spark",
//        name = "Test_4"
//      ))
//    )
//
//    KafkaServingMessage(
//      meta = Some(KafkaMessageMeta().withTraceId("traceId")),
//      requestOrError = RequestOrError.Request(req)
//    )
//
//  }
//
//  val txt = "He told the LA Times more police had to be called to the house because she was physically resisting arrest.\n\nThe officer said deputies had struggled to get Ms Locklear, a former star of US soap opera Melrose Place, into a police patrol car.\n\nVentura County Sheriff's Captain Garo Kuredjian said Ms Locklear's boyfriend had a physical injury, though he declined medical treatment.\n\nThe actress was taken to Ventura County Jail and released after posting $20,000 (£14,300) bail. She is due to appear in court on 13 March.\n\nMs Locklear, who was previously married to Bon Jovi guitarist Richie Sambora, first rose to fame as Sammy Jo Carrington in the 1980s TV show Dynasty.\n\nAs well as Melrose Place, she later appeared on TV police drama TJ Hooker and sitcom Spin City, for which she was twice nominated for a Golden Globe.\n\nThe actress had a previous brush with the authorities in 2008 when she was arrested on suspicion of driving under the influence of prescription medication.\n\nShe was fined $900 and sentenced to three years' informal probation after pleading guilty to reckless driving."
//
//}
