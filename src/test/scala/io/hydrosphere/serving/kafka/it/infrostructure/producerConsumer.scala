package io.hydrosphere.serving.kafka.it.infrostructure

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization._
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.kstream.{ForeachAction, KStream}

import scala.collection.mutable.ListBuffer

class TestProducer(hostAndPort:String, serializer:Class[_], deserializer:Class[_]) {
  val props = new Properties()
  props.put("bootstrap.servers", hostAndPort)
  props.put("key.serializer", serializer.getName)
  props.put("value.serializer", deserializer.getName)

  val producer = new KafkaProducer[Integer, String](props)

  def send(key: Integer, message: String) = {
    val record = new ProducerRecord("test", key, message)
    producer.send(record)
  }
}

class TestConsumer(hostAndPort:String, name:String, keySerializer:Class[_], valueSerializer:Class[_]) {
  val props = initProps()
  val (in, out, failure) = init()

  import org.apache.kafka.streams.StreamsConfig

  props.put(StreamsConfig.CLIENT_ID_CONFIG, name)
  props.put("group.id", name)
  props.put("bootstrap.servers", hostAndPort)
  props.put("key.deserializer", classOf[StringDeserializer].getName())
  props.put("value.deserializer", classOf[IntegerDeserializer].getName())

  def initProps() = {
    val p = new Properties()
    p.put(StreamsConfig.CLIENT_ID_CONFIG, name)
    p.put("group.id", name)
    p.put("bootstrap.servers", hostAndPort)
    p.put("key.deserializer", classOf[StringDeserializer].getName())
    p.put("value.deserializer", classOf[IntegerDeserializer].getName())
    p
  }

  def init(): (ListBuffer[String], ListBuffer[String], ListBuffer[String]) = {

    val successCollection = new ListBuffer[String]
    val failureCollection = new ListBuffer[String]
    val inCollection = new ListBuffer[String]

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

    (inCollection, successCollection, failureCollection)
  }
}
