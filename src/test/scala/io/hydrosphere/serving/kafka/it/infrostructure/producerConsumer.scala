package io.hydrosphere.serving.kafka.it.infrostructure

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization._
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.kstream.{ForeachAction, KStream}

import scala.collection.mutable.ListBuffer

class TestProducer[K,V](topic:String = "test", hostAndPort:String = "localhost:9092",
                        keySerializer:Class[_ <: Serializer[K]],
                        valSerializer:Class[_ <: Serializer[V]]) {
  val props = new Properties()
  props.put("bootstrap.servers", hostAndPort)
  props.put("key.serializer", keySerializer.getName)
  props.put("value.serializer", valSerializer.getName)

  val producer = new KafkaProducer[K, V](props)

  def send(key: K, message: V) = {
    val record = new ProducerRecord("test", key, message)
    producer.send(record)
  }
}

class TestConsumer[K,V](hostAndPort:String,
                        name:String,
                        keySerde:Class[_ <: Serde[K]],
                        valueSerde:Class[_ <: Serde[V]]) {
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

  def init(): (ListBuffer[V], ListBuffer[V], ListBuffer[V]) = {

    val successCollection = new ListBuffer[V]
    val failureCollection = new ListBuffer[V]
    val inCollection = new ListBuffer[V]

    val props = new Properties()
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test")
    props.put(StreamsConfig.CLIENT_ID_CONFIG, "test")
    import org.apache.kafka.streams.StreamsConfig
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, keySerde)
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valueSerde)

    import org.apache.kafka.streams.StreamsBuilder
    val builder = new StreamsBuilder()

    val s:KStream[K,V] = builder.stream("success")
    val f:KStream[K,V] = builder.stream("failure")
    val in:KStream[K,V] = builder.stream("test")

    s.foreach(new ForeachAction[K, V] {
      override def apply(key: K, value: V): Unit = {
        successCollection += value
      }
    })

    f.foreach(new ForeachAction[K, V] {
      override def apply(key: K, value: V): Unit = {
        failureCollection += value
      }
    })

    in.foreach(new ForeachAction[K, V] {
      override def apply(key: K, value: V): Unit ={
        inCollection += value
      }
    })

    val topology = builder.build()
    val streams = new KafkaStreams(topology, props)
    streams.start()

    (inCollection, successCollection, failureCollection)
  }
}
