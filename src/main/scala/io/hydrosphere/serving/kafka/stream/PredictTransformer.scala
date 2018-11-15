package io.hydrosphere.serving.kafka.stream

import io.hydrosphere.serving.kafka.config.Configuration
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.predict.{Application, PredictService}
import io.hydrosphere.serving.kafka.utils.KafkaMessageUtils._
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.streams.kstream.ValueTransformer
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}


class PredictTransformer( val predictService:PredictService,
                          val app:Application)(
                          implicit val config:Configuration,
                          val producer: Producer[Array[Byte], KafkaServingMessage])
  extends ValueTransformer[KafkaServingMessage, KafkaServingMessage]
  with Logging {

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  var ctx: ProcessorContext = _


  override def init(context: ProcessorContext): Unit = {
    ctx = context
  }

  override def transform(value: KafkaServingMessage): KafkaServingMessage = {
    val withKafka = value.forApplicaton(app)
      .withKafkaData(
        ctx.topic(),
        ctx.partition(),
        ctx.offset(),
        app.consumerId.getOrElse("UNKNOWN"))

    val allStages = predictService.predictByGraph(withKafka, app)

    allStages.foreach(_.foreach(shadowMe))

    Await.result(allStages.head, 1 hour) //TODO what actually timeout do we need
  }

  def shadowMe(message: KafkaServingMessage): Future[RecordMetadata] = {
    logger.debug(s"shadowing message: $message")
    producer.send(config.kafka.shadowTopic, message)
  }

  override def close(): Unit = {

  }
}