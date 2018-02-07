package io.hydrosphere.serving.kafka.config

import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.predict.PredictService
import io.hydrosphere.serving.kafka.stream.KafkaStreamer

final case class Context(config:Configuration,
                         kafkaServing:KafkaStreamer[Array[Byte], KafkaServingMessage],
                         predictService: PredictService)

object AppContext {

  def apply()(implicit config:Configuration,
            kafkaServing:KafkaStreamer[Array[Byte], KafkaServingMessage],
            predictService: PredictService) = Context(config,
    kafkaServing,
    predictService)

}
