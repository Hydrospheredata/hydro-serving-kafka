package io.hydrosphere.serving.kafka.stream

import java.util.Properties
import java.util.concurrent.Future

import io.hydrosphere.serving.kafka.config.Configuration
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.Serializer

object Producer {
  def apply[K, V](config:Configuration,
            keySerializer:Class[_ <: Serializer[K]],
            valSerializer:Class[_ <: Serializer[V]]) = new Producer[K, V](
    hostAndPort = s"${config.kafka.advertisedHost}:${config.kafka.advertisedPort}",
    keySerializer,
    valSerializer
  )
}

class Producer[K,V](hostAndPort:String,
                        keySerializer:Class[_ <: Serializer[K]],
                        valSerializer:Class[_ <: Serializer[V]]) {
  val props = new Properties()
  props.put("bootstrap.servers", hostAndPort)
  props.put("key.serializer", keySerializer.getName)
  props.put("value.serializer", valSerializer.getName)

  val producer = new KafkaProducer[K, V](props)

  def send(topic:String, key: K, message: V): Future[RecordMetadata] = {
    val record = new ProducerRecord(topic, key, message)
    producer.send(record)
  }

  def send(topic:String, message: V): Future[RecordMetadata] = {
    val record = new ProducerRecord[K, V](topic, message)
    producer.send(record)
  }

  def close():Unit = {
    producer.close()
  }
}

