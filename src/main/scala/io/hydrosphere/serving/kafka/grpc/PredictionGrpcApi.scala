package io.hydrosphere.serving.kafka.grpc

import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.stream.{KafkaStreamer, Producer}
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc.PredictionService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class PredictionGrpcApi(implicit streamer: KafkaStreamer[Array[Byte], KafkaServingMessage],
                       producer:Producer[Array[Byte], KafkaServingMessage]
                       ) extends PredictionService with Logging {

  implicit val ctx = scala.concurrent.ExecutionContext.global

  override def predict(request: PredictRequest): Future[PredictResponse] =
    request.modelSpec match {
      case Some(modelSpec) => {
        val sent = streamer
          .inTopicsByApplicationName(modelSpec.name)
          .map(producer.send(_, KafkaServingMessage().withRequest(request)))

        if(sent.isEmpty) throw new IllegalArgumentException(s"there are no configured in topics for ")

        Future.sequence(sent).map {
          sendReports =>
            logger.info(s"message for application ${modelSpec.name} successfully published to ${
              sendReports
                .map(rm => s"topic:${rm.topic()}, partition:${rm.partition()}, offset:${rm.offset()}\n")
                .foldLeft("")(_+_)
            }")
            PredictResponse()
        }
      }
      case None => Future.failed(new IllegalArgumentException("ModelSpec is not defined"))
  }

}
