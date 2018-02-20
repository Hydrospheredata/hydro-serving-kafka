package io.hydrosphere.serving.kafka.it.infrostructure

import java.util.Properties
import org.apache.kafka.common.serialization._
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.kstream.{ForeachAction, KStream}

import scala.collection.mutable.ListBuffer

class TestConsumer[K,V](hostAndPort:String,
                        name:String,
                        keySerde:Class[_ <: Serde[K]],
                        valueSerde:Class[_ <: Serde[V]]) {
  val props: Properties = initProps()
  val (in, out, failure, shadow) = init()
  var streams :KafkaStreams = _

  import org.apache.kafka.streams.StreamsConfig

  def initProps(): Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "test")
    p.put(StreamsConfig.CLIENT_ID_CONFIG, "test")
    import org.apache.kafka.streams.StreamsConfig
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, hostAndPort)
    p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, keySerde)
    p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valueSerde)
    p
  }

  def init(): (ListBuffer[V], ListBuffer[V], ListBuffer[V], ListBuffer[V]) = {

    val successCollection = new ListBuffer[V]
    val failureCollection = new ListBuffer[V]
    val inCollection = new ListBuffer[V]
    val shadowCollection = new ListBuffer[V]

    import org.apache.kafka.streams.StreamsBuilder
    val builder = new StreamsBuilder()

    val s:KStream[K,V] = builder.stream("success")
    val f:KStream[K,V] = builder.stream("failure")
    val in:KStream[K,V] = builder.stream("test")
    val shadow:KStream[K,V] = builder.stream("shadow_topic")

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

    shadow.foreach(new ForeachAction[K, V] {
      override def apply(key: K, value: V): Unit ={
        shadowCollection += value
      }
    })

    val topology = builder.build()
    streams = new KafkaStreams(topology, props)
    streams.start()

    (inCollection, successCollection, failureCollection, shadowCollection)
  }

  def close(): Unit = if(streams != null){
      streams.close()
  }


}
