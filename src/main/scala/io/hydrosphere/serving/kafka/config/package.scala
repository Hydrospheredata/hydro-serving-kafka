package io.hydrosphere.serving.kafka

import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.stream.KafkaStreamer

package object config {
  type KafkaServingStream =  KafkaStreamer[Array[Byte], KafkaServingMessage]
}
