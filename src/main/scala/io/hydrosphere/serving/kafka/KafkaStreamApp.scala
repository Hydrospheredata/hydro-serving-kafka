package io.hydrosphere.serving.kafka


import io.hydrosphere.serving.kafka.config.KafkaServingStream
import org.apache.logging.log4j.scala.Logging
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.predict._
import io.hydrosphere.serving.manager.grpc.applications.ExecutionGraph
import io.hydrosphere.serving.tensorflow.api.predict.PredictRequest

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
    predictor: PredictService
  ):Flow = {
    val flow = new Flow()
    flow.start()
    flow
  }
}

class Flow()(
  implicit kafkaServing: KafkaServingStream,
  applicationUpdateService: UpdateService[Seq[Application]],
  predictor: PredictService
) extends Logging {

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global



  import java.util.concurrent.CountDownLatch

  private[this] val latch = new CountDownLatch(1)

  def start(): Unit = {
    logger.info("starting kafka serving app")

    kafkaServing
      .streamForAll[KafkaServingMessage]{ appAndStream =>

      val app = appAndStream._1

      val graph = appAndStream._1.executionGraph.getOrElse(ExecutionGraph(Seq()))

      appAndStream._2
        .filterV(_.requestOrError.isRequest)
        .mapV(message => (message.traceId, PredictRequest(None, message.requestOrError.request.get.inputs)))
        .mapV(message => (message._1, predictor.predictByGraph(message._2, app)))
        .mapAsync(message => predictor.report(message._1, message._2))
        .branchV(_.requestOrError.isRequest, _.requestOrError.isError)
    }

    latch.await()
  }

  def stop(): Unit = {
    kafkaServing.stop()

    latch.countDown()
  }

}
