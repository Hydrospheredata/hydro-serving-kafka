package io.hydrosphere.serving.kafka.config

import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._
import pureconfig.generic.auto._


final case class KafkaConfiguration(
  advertisedHost: String = "kafka",
  advertisedPort: Int = 9092,
  shadowTopic: String = "shadow_topic"
)
final case class ApplicationConfig(
  appId: String = "hydro-serving-kafka",
)

final case class SidecarConfig(
  host: String = "sidecar",
  port: Int = 8080,
  xdsSilentRestartSeconds: Int = 30
)
final case class GrpcConfig(
  port: Int = 9091,
  maxMessageSize: Int = 512 * 1024 * 1024,
  deadline: Duration = 5.minutes
)
final case class Configuration(
  application: ApplicationConfig,
  sidecar: SidecarConfig,
  kafka: KafkaConfiguration,
  grpc: GrpcConfig
)

object Configuration extends Logging {
  def load = {
    pureconfig.loadConfig[Configuration]
  }

  def loadOrFail: Configuration = {
    load.right.get
  }
}