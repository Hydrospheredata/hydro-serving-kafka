package io.hydrosphere.serving.kafka.stream

import java.util.Properties

import io.hydrosphere.serving.kafka.config.Configuration
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.Serializer

import scala.concurrent.{Future, Promise}

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
    val promise = Promise[RecordMetadata]
    producer.send(record, new Published(promise))
    promise.future
  }

  def send(topic:String, message: V): Future[RecordMetadata] = {
    val record = new ProducerRecord[K, V](topic, message)
    val promise = Promise[RecordMetadata]
    producer.send(record, new Published(promise))
    promise.future
  }

  def close():Unit = {
    producer.close()
  }

  private class Published(promise:Promise[RecordMetadata]) extends Callback {
    override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
      if(exception != null) promise.failure(exception)
      else promise.success(metadata)
    }
  }
}

