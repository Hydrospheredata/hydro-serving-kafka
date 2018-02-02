package io.hydrosphere.serving.kafka.config

import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging


case class KafkaConfiguration(advertisedHost: String, advertisedPort: Int)
case class ApplicationConfig(port: Int, appId: String)
case class SidecarConfig(host: String, port: Int)

final case class Configuration(application:ApplicationConfig,
                             sidecar:SidecarConfig,
                             kafka:KafkaConfiguration)

object Configuration extends Logging {

    private[Configuration] def logged[T](configName:String)(wrapped:T):T = {
      logger.info(s"configuration: $configName: $wrapped")
      wrapped
    }

    def parseSidecar(config: Config): SidecarConfig = logged("sidecar"){
      val c = config.getConfig("sidecar")
      SidecarConfig(
        host = c.getString("host"),
        port = c.getInt("port")
      )
    }

    def parseApplication(config: Config): ApplicationConfig = logged("base app"){
      val c = config.getConfig("application")
      ApplicationConfig(
        port = c.getInt("port"),
        appId = c.getString("appId")
      )
    }

    def parseKafka(config: Config): KafkaConfiguration = logged("kafka"){
      val c = config.getConfig("kafka")
      KafkaConfiguration(
        advertisedHost = c.getString("advertisedHost"),
        advertisedPort = c.getInt("advertisedPort")
      )
    }

  def apply(config:Config): Configuration = Configuration(
    parseApplication(config),
    parseSidecar(config),
    parseKafka(config)
  )

}



