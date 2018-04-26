package io.hydrosphere.serving.kafka.grpc

import io.hydrosphere.serving.grpc.{Header, Headers}
import io.hydrosphere.serving.kafka.kafka_messages.{KafkaMessageMeta, KafkaServingMessage}
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
    
    val topic = Option(Headers.KafkaTopic.contextKey.get())
      .getOrElse(throw new IllegalArgumentException("Specify topic"))
    

    def getHeaderValue(header: Header): String = Option(header.contextKey.get()).getOrElse("empty")
    
    val meta = KafkaMessageMeta(
      getHeaderValue(Headers.TraceId),
      getHeaderValue(Headers.ApplicationId),
      getHeaderValue(Headers.StageId),
      getHeaderValue(Headers.StageName),
      None
    )

    logger.info(s"Sending new message with meta: $meta")
    
    producer.send(topic, KafkaServingMessage().withRequest(request).withMeta(meta))
      .map(_ => PredictResponse())
  }

}
