package io.hydrosphere.serving.kafka.grpc

import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.stream.{AppStreamer, Producer, Streamer}
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc.PredictionService
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future

class PredictionGrpcApi(implicit streamer: AppStreamer[Array[Byte], KafkaServingMessage],
                        producer: Producer[Array[Byte], KafkaServingMessage]
                       ) extends PredictionService with Logging {

  implicit val ctx = scala.concurrent.ExecutionContext.global

  override def predict(request: PredictRequest): Future[PredictResponse] =
    request.modelSpec match {
      case Some(modelSpec) => {
        val sent = streamer
          .itemsById(modelSpec.name)
          .view
          .map(_.inTopic)
          .filter(_.isDefined)
          .map(_.get)
          .map(producer.send(_, KafkaServingMessage().withRequest(request)))
          .toSet

        if (sent.isEmpty)
          return Future.failed(new IllegalArgumentException(s"there are no configured in topics for application: $modelSpec"))

        val seq: Future[scala.collection.Set[RecordMetadata]] = Future.sequence(sent)

        Future.sequence(sent).map { sentReports =>
          val topicsReport = messageToLog(sentReports)
          logger.info(s"message for application ${modelSpec.name} successfully published to $topicsReport")
          PredictResponse()
        }
      }

      case None => Future.failed(new IllegalArgumentException("ModelSpec is not defined"))
    }

  def messageToLog(sentReports: scala.collection.Set[RecordMetadata]) = sentReports
    .map(rm => s"topic:${rm.topic()}, partition:${rm.partition()}, offset:${rm.offset()}\n")
    .foldLeft("")(_ + _)

}
