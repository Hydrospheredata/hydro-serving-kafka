package io.hydrosphere.serving.kafka.utils

import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage.RequestOrError
import io.hydrosphere.serving.kafka.kafka_messages.{KafkaError, KafkaServingMessage}

object KafkaMessageUtils {

  def withException(e:Throwable):KafkaServingMessage = KafkaServingMessage(
    meta = None,
    requestOrError = RequestOrError.Error(KafkaError(
      e.getMessage, None
    ))
  )

  def withException(errorMessage:String):KafkaServingMessage = KafkaServingMessage(
    meta = None,
    requestOrError = RequestOrError.Error(KafkaError(
      errorMessage, None
    ))
  )

}
