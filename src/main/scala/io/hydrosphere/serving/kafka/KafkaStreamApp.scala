package io.hydrosphere.serving.kafka


import io.grpc.{Server, ServerBuilder, ServerInterceptor, ServerServiceDefinition}
import io.hydrosphere.serving.grpc.Headers
import io.hydrosphere.serving.kafka.config.{Configuration, KafkaServingStream}
import io.hydrosphere.serving.kafka.grpc.PredictionGrpcApi
import org.apache.logging.log4j.scala.Logging
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.predict._
import io.hydrosphere.serving.kafka.stream.{PredictTransformer, Producer}
import io.hydrosphere.serving.manager.grpc.applications.ExecutionGraph
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
    config: Configuration,
    producer: Producer[Array[Byte], KafkaServingMessage]
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
  producer: Producer[Array[Byte], KafkaServingMessage]
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


    case class BuilderWrapper[T <: ServerBuilder[T]](builder: ServerBuilder[T]) {
      def addService(service: ServerServiceDefinition): BuilderWrapper[T] = {
        BuilderWrapper(builder.addService(service))
      }

      def intercept(service: ServerInterceptor): BuilderWrapper[T] = {
        BuilderWrapper(builder.intercept(service))
      }

      def build: Server = {
        builder.build()
      }
    }

    val builder = BuilderWrapper(ServerBuilder.forPort(config.application.port))
      .addService(PredictionServiceGrpc.bindService(predictionApi, scala.concurrent.ExecutionContext.global))
      .intercept(Headers.KafkaTopic.interceptor)
      .intercept(Headers.ApplicationId.interceptor)
      .intercept(Headers.TraceId.interceptor)
      .intercept(Headers.StageId.interceptor)
      .intercept(Headers.StageName.interceptor)
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
