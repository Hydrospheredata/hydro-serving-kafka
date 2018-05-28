package io.hydrosphere.serving.kafka.mappers

import java.util

import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import io.hydrosphere.serving.kafka.utils.KafkaMessageUtils._
import io.hydrosphere.serving.monitoring.monitoring.ExecutionInformation

import scala.util.Try


class KafkaMonitoringMessageSerializer extends Serializer[ExecutionInformation]{
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
  override def serialize(topic: String, data: ExecutionInformation): Array[Byte] = {
    data.toByteArray
  }
  override def close(): Unit = {}
}

class KafkaServingMessageSerializer extends Serializer[KafkaServingMessage]{
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
  override def serialize(topic: String, data: KafkaServingMessage): Array[Byte] = {
    data.toByteArray
  }
  override def close(): Unit = {}
}

class KafkaServingMessageDeserializer extends Deserializer[KafkaServingMessage] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
  override def close(): Unit = {}
  override def deserialize(topic: String, data: Array[Byte]): KafkaServingMessage = Try{
    KafkaServingMessage.parseFrom(data)
  } recover {case e:Throwable => withException(s"message parse exception: ${e.getMessage}")} get
}

class KafkaServingMessageSerde extends Serde[KafkaServingMessage] {
  override def deserializer(): Deserializer[KafkaServingMessage] = new KafkaServingMessageDeserializer
  override def serializer(): Serializer[KafkaServingMessage] = new KafkaServingMessageSerializer
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}
  override def close(): Unit = {}
}
