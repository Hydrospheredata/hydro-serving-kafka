package io.hydrosphere.serving.kafka

import io.hydrosphere.serving.kafka.config.AppContext
import org.apache.logging.log4j.scala.Logging

object Application extends App with Logging {

  start(AppContext())

  def start(context:AppContext): Unit = {
    logger.info("starting kafka serving app")

    sys addShutdownHook {
      logger.info("Stopping kafka serving app")
      //TODO cleanup
    }
  }

}
