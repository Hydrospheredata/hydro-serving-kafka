package io.hydrosphere.serving.kafka


import io.hydrosphere.serving.kafka.config.{Configuration, KafkaServingStream}
import org.apache.logging.log4j.scala.Logging
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.predict._
import io.hydrosphere.serving.kafka.stream.{PredictTransformer, Producer}
import io.hydrosphere.serving.manager.grpc.applications.ExecutionGraph

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
    config: Configuration,
    producer: Producer[Array[Byte], KafkaServingMessage]
  ):Flow = {
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
  producer: Producer[Array[Byte], KafkaServingMessage]
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
        .transformV(() => new PredictTransformer(predictor, app))
        .branchV(_.requestOrError.isRequest, _.requestOrError.isError)
    }

    latch.await()
  }

  def stop(): Unit = {
    kafkaServing.stop()
    producer.close()
    latch.countDown()
  }

}
