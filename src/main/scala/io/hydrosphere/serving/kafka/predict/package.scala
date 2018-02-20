package io.hydrosphere.serving.kafka

import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage

import scala.concurrent.Future

package object predict {
 type Version = String
 type FutureM = Future[KafkaServingMessage]
 type FutureMList = List[FutureM]
}
