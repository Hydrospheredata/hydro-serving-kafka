package io.hydrosphere.serving.kafka.stream

import io.hydrosphere.serving.kafka.config.Configuration
import io.hydrosphere.serving.kafka.kafka_messages.{KafkaMessageLocation, KafkaMessageMeta, KafkaServingMessage}
import io.hydrosphere.serving.kafka.predict.{Application, PredictService}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.streams.kstream.ValueTransformer
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration._


class PredictTransformer(
                          val predictService:PredictService,
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
    val meta = value.meta.getOrElse(KafkaMessageMeta())
      .withApplicationId(app.name)
      .withLocation(KafkaMessageLocation(
        sourceTopic = ctx.topic(),
        partition = ctx.partition(),
        offset = ctx.offset(),
        consumerId = app.consumerId.getOrElse("UNKNOWN")
      ))

    val allStages = predictService.predictByGraph(value.withMeta(meta), app)

    allStages.foreach(_.foreach(shadowMe))

    Await.result(allStages.head, 1 hour) //TODO what actually timeout do we need
  }

  def shadowMe(message: KafkaServingMessage) = {
    logger.debug(s"shadowing message: $message")
    producer.send(config.kafka.shadowTopic, message)
  }

  override def close(): Unit = {

  }

  override def punctuate(timestamp: Long): KafkaServingMessage = ???
}
