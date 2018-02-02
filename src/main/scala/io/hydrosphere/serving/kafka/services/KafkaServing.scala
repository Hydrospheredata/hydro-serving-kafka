package io.hydrosphere.serving.kafka.services

import io.hydrosphere.serving.kafka.config.KafkaConfiguration

trait KafkaServing {

  def init(kafkaConfiguration: KafkaConfiguration)
  def stop():Unit


}

object KafkaServing extends KafkaServing {
  override def init(kafkaConfiguration: KafkaConfiguration): Unit = ???

  override def stop(): Unit = ???
}
