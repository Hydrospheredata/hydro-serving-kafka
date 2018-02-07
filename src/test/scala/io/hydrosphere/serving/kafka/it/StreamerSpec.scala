package io.hydrosphere.serving.kafka.it

import java.util.Properties
import java.util.concurrent.TimeUnit

import io.hydrosphere.serving.kafka.config.{ApplicationConfig, Configuration, KafkaConfiguration, SidecarConfig}
import io.hydrosphere.serving.kafka.it.infrostructure.KafkaContainer
import io.hydrosphere.serving.kafka.predict.Application
import io.hydrosphere.serving.kafka.stream.KafkaStreamer
import io.hydrosphere.serving.manager.grpc.applications.ExecutionGraph
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization._
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.kstream.{ForeachAction, KStream}
import org.apache.log4j.BasicConfigurator
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer

class StreamerSpec extends FlatSpec
  with Matchers
  with KafkaContainer
  with BeforeAndAfter {

  "kafka container" should "topic should be created" in {

    createTopic("success")
    createTopic("failure")
    createTopic("test")

    BasicConfigurator.configure()

    implicit val conf = Configuration(
      ApplicationConfig(1, "123"),
      SidecarConfig("1", 1),
      KafkaConfiguration("localhost", 9092)
    )

    TestConsumer.init()


    val streamer = new KafkaStreamer[Integer, String](Serdes.Integer().getClass, Serdes.String().getClass)
    val modelSpec = ModelSpec()
    val app = Application("someName", Some("test"), Some("success"), Some("failure"), modelSpec, ExecutionGraph())


        new Thread(new Runnable {
          override def run(): Unit =
        streamer.streamFor[String](app, { appAndStream =>
          appAndStream
            ._2.mapV({value =>
            log.info(s"mapping: ${value}")
            value.toUpperCase()
          })
            .branchV(_.startsWith("SUCCESS"), _.startsWith("FAILURE"))
        })
        }).start()

    new Thread(new Runnable {
      override def run(): Unit = {
        Array(TestProducer.send(1, "success_1"),
          TestProducer.send(2, "success_2"),
          TestProducer.send(3, "success_3"),
          TestProducer.send(4, "failure_1"),
          TestProducer.send(5, "failure_2"),
          TestProducer.send(6, "failure_3")
        ).map(f => log.info(s"!!!!!!! sent ${f.get()}"))

      }
    }).start()


    TimeUnit.SECONDS.sleep(3)

    val in = TestConsumer.inCollection
    val success = TestConsumer.successCollection
    val failure = TestConsumer.failureCollection
    //  streamer.stop()

    topicDelete("success")
    topicDelete("failure")
    topicDelete("test")

  }

  object TestConsumer {
    val props = new Properties()

    import org.apache.kafka.streams.StreamsConfig

    props.put(StreamsConfig.CLIENT_ID_CONFIG, "test1")
    props.put("group.id", "test1")
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.deserializer", classOf[StringDeserializer].getName())
    props.put("value.deserializer", classOf[IntegerDeserializer].getName())

    val successCollection = new ListBuffer[String]
    val failureCollection = new ListBuffer[String]
    val inCollection = new ListBuffer[String]

    def init(): Unit = {
      val props = new Properties()
      props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test")
      props.put(StreamsConfig.CLIENT_ID_CONFIG, "test")
      import org.apache.kafka.streams.StreamsConfig
      props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
      props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, classOf[Serdes.IntegerSerde])
      props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, classOf[Serdes.StringSerde])

      import org.apache.kafka.streams.StreamsBuilder
      val builder = new StreamsBuilder()

      val s:KStream[Int,String] = builder.stream("success")
      val f:KStream[Int,String] = builder.stream("failure")
      val in:KStream[Int,String] = builder.stream("test")

      s.foreach(new ForeachAction[Int, String] {
        override def apply(key: Int, value: String): Unit = {
          successCollection += value
        }
      })

      f.foreach(new ForeachAction[Int, String] {
        override def apply(key: Int, value: String): Unit = {
          failureCollection += value
        }
      })

      in.foreach(new ForeachAction[Int, String] {
        override def apply(key: Int, value: String): Unit ={
          inCollection += value
        }
      })

      val topology = builder.build()
      val streams = new KafkaStreams(topology, props)
      streams.start()

    }
  }

}

object TestProducer {
  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", classOf[IntegerSerializer].getName)
  props.put("value.serializer", classOf[StringSerializer].getName)

  val producer = new KafkaProducer[Integer, String](props)

  def send(key: Integer, message: String) = {
    val record = new ProducerRecord("test", key, message)
    producer.send(record)
  }
}


