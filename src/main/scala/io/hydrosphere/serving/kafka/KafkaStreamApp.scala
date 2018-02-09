package io.hydrosphere.serving.kafka

import com.typesafe.config.ConfigFactory
import io.grpc.ManagedChannelBuilder
import io.hydrosphere.serving.kafka.config.{AppContext, Configuration, Context}
import org.apache.logging.log4j.scala.Logging
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.mappers.KafkaServingMessageSerde
import io.hydrosphere.serving.kafka.predict.{Application, ApplicationService, PredictService, PredictServiceImpl}
import io.hydrosphere.serving.kafka.stream.KafkaStreamer
import io.hydrosphere.serving.tensorflow.api.predict.PredictRequest
import org.apache.kafka.common.serialization.Serdes

import scala.concurrent.ExecutionContext

object KafkaStreamApp extends App with Logging {

  implicit val appConfig = Configuration(ConfigFactory.load())

  implicit val appService = new ApplicationService {
    override def getApplications(): Seq[Application] = Seq()
  }


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



  import java.util.concurrent.CountDownLatch

  private[this] val latch = new CountDownLatch(1)

  def start(context: Context): Unit = {
    logger.info("starting kafka serving app")

    val kafkaServing = context.kafkaServing
    val predictor = context.predictService

    context.kafkaServing
      .streamForAll[KafkaServingMessage] { appAndStream =>

      val app = appAndStream._1

      appAndStream._2
        .filterV(_.requestOrError.isRequest)
        .mapV(message => (message.traceId, PredictRequest(Some(appAndStream._1.modelSpec), message.requestOrError.request.get.inputs)))
        .mapV(message => (message._1, predictor.predictByGraph(message._2, app.executionGraph)))
        .mapAsync(value => predictor.report(value._1, value._2))
        .branchV(_.requestOrError.isRequest, _.requestOrError.isError)
    }

    latch.await
  }

  def stop(): Unit ={
    latch.countDown()
  }
}
