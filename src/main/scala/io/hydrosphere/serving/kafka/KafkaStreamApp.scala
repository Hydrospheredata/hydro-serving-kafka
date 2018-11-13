package io.hydrosphere.serving.kafka

import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object KafkaStreamApp extends App with Logging {

  import io.hydrosphere.serving.kafka.config.Inject._

  logger.info("Starting kafka serving app")

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  val flow = Flow()

  sys addShutdownHook {
    logger.info("Stopping kafka serving app")
    flow.stop()
  }
}