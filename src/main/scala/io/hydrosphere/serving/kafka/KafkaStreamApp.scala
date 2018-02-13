package io.hydrosphere.serving.kafka

import com.typesafe.config.ConfigFactory
import io.grpc.ManagedChannelBuilder
import io.hydrosphere.serving.kafka.config.{AppContext, Configuration, Context}
import org.apache.logging.log4j.scala.Logging
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.mappers.KafkaServingMessageSerde
import io.hydrosphere.serving.kafka.predict.{PredictService, PredictServiceImpl, XDSApplicationUpdateService}
import io.hydrosphere.serving.kafka.stream.KafkaStreamer
import io.hydrosphere.serving.manager.grpc.applications.ExecutionGraph
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import org.apache.kafka.common.serialization.Serdes

import scala.concurrent.ExecutionContext

object KafkaStreamApp extends App with Logging {

  implicit val appConfig = Configuration(ConfigFactory.load())



  implicit val kafkaServing = new KafkaStreamer[Array[Byte], KafkaServingMessage](Serdes.ByteArray().getClass, classOf[KafkaServingMessageSerde])
  implicit val rpcChanel = ManagedChannelBuilder
    .forAddress(appConfig.sidecar.host, appConfig.sidecar.port)
    .usePlaintext(true)
    .build


  implicit val predictService: PredictService = new PredictServiceImpl


  implicit val applicationUpdater = new XDSApplicationUpdateService()

  Flow.start(AppContext())

  sys addShutdownHook {
    kafkaServing.stop()
    logger.info("Stopping kafka serving app")
  }
}


object Flow extends Logging {

  implicit val executionContext = ExecutionContext.global



  import java.util.concurrent.CountDownLatch

  private[this] val latch = new CountDownLatch(1)

  def start(context: Context): Unit = {
    logger.info("starting kafka serving app")

    val kafkaServing = context.kafkaServing
    val predictor = context.predictService

    context.kafkaServing
      .streamForAll[KafkaServingMessage](context.applicationUpdateService) { appAndStream =>

      val app = appAndStream._1

      val graph = appAndStream._1.executionGraph.getOrElse(ExecutionGraph(Seq()))

      appAndStream._2
        .filterV(_.requestOrError.isRequest)
        .mapV(message => (message.traceId, PredictRequest(None, message.requestOrError.request.get.inputs)))
        .mapV(message => (message._1, predictor.predictByGraph(app.name, message._2, graph)))
        .mapAsync(message => predictor.report(message._1, message._2))
        .branchV(_.requestOrError.isRequest, _.requestOrError.isError)
    }

    latch.await
  }

  def stop(context: Context): Unit = {
    context.kafkaServing.stop()
    latch.countDown()
  }
}
