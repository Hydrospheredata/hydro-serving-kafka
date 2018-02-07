package io.hydrosphere.serving.kafka

import com.typesafe.config.ConfigFactory
import io.grpc.ManagedChannelBuilder
import io.hydrosphere.serving.kafka.config.{AppContext, Configuration, Context}
import org.apache.logging.log4j.scala.Logging
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.mappers.KafkaServingMessageSerde
import io.hydrosphere.serving.kafka.predict.{PredictService, PredictServiceImpl}
import io.hydrosphere.serving.kafka.stream.KafkaStreamer
import io.hydrosphere.serving.tensorflow.api.predict.PredictRequest
import org.apache.kafka.common.serialization.Serdes

import scala.concurrent.ExecutionContext

object KafkaStreamApp extends App with Logging {

  implicit val appConfig = Configuration(ConfigFactory.load())
  implicit val kafkaServing = new KafkaStreamer[Array[Byte], KafkaServingMessage](Serdes.ByteArray().getClass, classOf[KafkaServingMessageSerde])
  implicit val modelsChannel = ManagedChannelBuilder
    .forAddress(appConfig.sidecar.host, appConfig.sidecar.port)
    .usePlaintext(true)
    .build

  implicit val predictService: PredictService = new PredictServiceImpl

  Flow.start(AppContext())

  sys addShutdownHook {
    kafkaServing.stop()
    logger.info("Stopping kafka serving app")
  }
}


object Flow extends Logging {

  implicit val executionContext = ExecutionContext.global

  def start(context: Context): Unit = {
    logger.info("starting kafka serving app")

    val kafkaServing = context.kafkaServing
    val predictor = context.predictService

    context.kafkaServing
      .streamForAll[KafkaServingMessage] { appAndStream =>

      val app = appAndStream._1

      appAndStream._2
        .filterV(_.requestOrError.isRequest)
        .mapV(message => PredictRequest(Some(appAndStream._1.modelSpec), message.requestOrError.request.get.inputs))
        .mapV(predictor.predictByGraph(_, app.executionGraph))
        .mapAsync(predictor.report(_))
        .branchV(_.requestOrError.isRequest, _.requestOrError.isError)
    }
  }
}
