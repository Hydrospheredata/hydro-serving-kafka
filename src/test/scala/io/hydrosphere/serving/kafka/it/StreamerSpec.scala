//package io.hydrosphere.serving.kafka.it
//
//import java.util.Properties
//import java.util.concurrent.TimeUnit
//
//import io.hydrosphere.serving.kafka.config.{ApplicationConfig, Configuration, KafkaConfiguration, SidecarConfig}
//import io.hydrosphere.serving.kafka.it.infrostructure.{KafkaContainer, TestConsumer, TestProducer}
//import io.hydrosphere.serving.kafka.predict.{Application}
//import io.hydrosphere.serving.kafka.stream.KafkaStreamer
//import io.hydrosphere.serving.manager.grpc.applications.ExecutionGraph
//import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
//import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
//import org.apache.kafka.common.serialization._
//import org.apache.kafka.streams.KafkaStreams
//import org.apache.kafka.streams.kstream.{ForeachAction, KStream}
//import org.apache.log4j.BasicConfigurator
//import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
//
//import scala.collection.mutable.ListBuffer
//
//class StreamerSpec extends FlatSpec
//  with Matchers
//  with KafkaContainer
//  with BeforeAndAfter {
//
//  "kafka container" should "topic should be created" in {
//
//    createTopic("success")
//    createTopic("failure")
//    createTopic("test")
//
//    BasicConfigurator.configure()
//
//    implicit val conf = Configuration(
//      ApplicationConfig(1, "123"),
//      SidecarConfig("1", 1),
//      KafkaConfiguration("localhost", 9092)
//    )
//
//    val consumer = new TestConsumer("localhost:9092",
//      "test_1",
//      Serdes.Integer().getClass,
//      Serdes.String().getClass)
//
//
//    val streamer = new KafkaStreamer[Integer, String](Serdes.Integer().getClass, Serdes.String().getClass)
//    val modelSpec = ModelSpec()
//    val app = Application("someName", Some("test"), Some("success"), Some("failure"), modelSpec, ExecutionGraph())
//
//
//        new Thread(new Runnable {
//          override def run(): Unit =
//        streamer.streamFor[String](app) { appAndStream =>
//          appAndStream
//            ._2.mapV({value =>
//            log.info(s"mapping: ${value}")
//            value.toUpperCase()
//          })
//            .branchV(_.startsWith("SUCCESS"), _.startsWith("FAILURE"))
//        })
//        }).start()
//
//    new Thread(new Runnable {
//      override def run(): Unit = {
//
//          val producer = new TestProducer[Integer, String](hostAndPort = "localhost:9092",
//            keySerializer = classOf[IntegerSerializer],
//            valSerializer = classOf[StringSerializer])
//
//        Array(producer.send(1, "success_1"),
//          producer.send(2, "success_2"),
//          producer.send(3, "success_3"),
//          producer.send(4, "failure_1"),
//          producer.send(5, "failure_2"),
//          producer.send(6, "failure_3")
//        ).map(f => log.info(s"!!!!!!! sent ${f.get()}"))
//
//      }
//    }).start()
//
//
//    TimeUnit.SECONDS.sleep(3)
//
//
//    topicDelete("success")
//    topicDelete("failure")
//    topicDelete("test")
//
//  }
//}
//
//
//
