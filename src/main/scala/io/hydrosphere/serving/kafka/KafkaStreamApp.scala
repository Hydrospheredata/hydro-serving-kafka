package io.hydrosphere.serving.kafka


import io.grpc.{Server, ServerBuilder}
import io.hydrosphere.serving.grpc.{BuilderWrapper, Headers}
import io.hydrosphere.serving.kafka.config.{Configuration, KafkaServingStream}
import io.hydrosphere.serving.kafka.grpc.{MonitoringServiceGrpcApi, PredictionGrpcApi}
import org.apache.logging.log4j.scala.Logging
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.predict._
import io.hydrosphere.serving.kafka.stream.{PredictTransformer, Producer}
import io.hydrosphere.serving.manager.grpc.applications.ExecutionGraph
import io.hydrosphere.serving.monitoring.monitoring.{ExecutionInformation, MonitoringServiceGrpc}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object KafkaStreamApp extends App with Logging {

  import io.hydrosphere.serving.kafka.config.Inject._

  logger.info("Starting kafka serving app")

  val flow = Flow()

  sys addShutdownHook {
    logger.info("Stopping kafka serving app")
    flow.stop()
  }

}


object Flow {
  def apply()(
    implicit kafkaServing: KafkaServingStream,
    applicationUpdateService: UpdateService[Seq[Application]],
    predictor: PredictService,
    predictionApi: PredictionGrpcApi,
    monitoringApi: MonitoringServiceGrpcApi,
    config: Configuration,
    producer: Producer[Array[Byte], KafkaServingMessage],
    monitoringProducer: Producer[Array[Byte], ExecutionInformation]
  ): Flow = {
    val flow = new Flow()
    flow.start()
    flow
  }
}

class Flow()(
  implicit kafkaServing: KafkaServingStream,
  applicationUpdateService: UpdateService[Seq[Application]],
  predictor: PredictService,
  config: Configuration,
  predictionApi: PredictionGrpcApi,
  monitoringApi: MonitoringServiceGrpcApi,
  producer: Producer[Array[Byte], KafkaServingMessage],
  monitoringProducer: Producer[Array[Byte], ExecutionInformation]
) extends Logging {

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global


  private[this] var server: Server = _

  def start(): Unit = {
    logger.info("starting kafka serving app")
    kafkaServing
      .streamForAll[KafkaServingMessage] { appAndStream =>

      val app = appAndStream._1

      val graph = appAndStream._1.executionGraph.getOrElse(ExecutionGraph(Seq()))

      appAndStream._2
        .filterV(_.requestOrError.isRequest)
        .transformV(() => new PredictTransformer(predictor, app))
        .branchV(_.requestOrError.isRequest, _.requestOrError.isError)
    }


    val builder = BuilderWrapper(ServerBuilder.forPort(config.application.port))
      .addService(PredictionServiceGrpc.bindService(predictionApi, scala.concurrent.ExecutionContext.global))
      .addService(MonitoringServiceGrpc.bindService(monitoringApi, scala.concurrent.ExecutionContext.global))
      .intercept(Headers.XServingKafkaProduceTopic.interceptor)
    server = builder.build

    server.start()
    logger.info(s"server on port ${server.getPort} started")
    server.awaitTermination()

  }

  def stop(): Unit = {
    if (!server.isShutdown) {
      val port = server.getPort
      server.shutdownNow()
      logger.info(s"grpc server on port $port stopped")
    }

    if (!server.isTerminated) {
      server.awaitTermination()
    }
    // latch.countDown()
  }

}
