package io.hydrosphere.serving.kafka.config

import com.typesafe.config.ConfigFactory
import io.hydrosphere.serving.kafka.services.KafkaServing

final case class AppContext(config:Configuration, kafkaServing:KafkaServing)

object AppContext {

  def apply(): AppContext = {
    val appConfig = Configuration(ConfigFactory.load())
    new AppContext(appConfig, KafkaServing)
  }

}
