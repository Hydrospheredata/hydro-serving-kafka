package io.hydrosphere.serving.kafka.grpc

import io.hydrosphere.serving.grpc.Headers
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.stream.{AppStreamer, Producer}
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc.PredictionService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future

class PredictionGrpcApi(implicit streamer: AppStreamer[Array[Byte], KafkaServingMessage],
  producer: Producer[Array[Byte], KafkaServingMessage]
) extends PredictionService with Logging {

  implicit val ctx = scala.concurrent.ExecutionContext.global

  override def predict(request: PredictRequest): Future[PredictResponse] = {
    
    val topic = Option(Headers.XServingKafkaProduceTopic.contextKey.get())
      .getOrElse(throw new IllegalArgumentException("Specify topic"))
    
    producer.send(topic, KafkaServingMessage().withRequest(request))
      .map(_ => PredictResponse())
  }

}
