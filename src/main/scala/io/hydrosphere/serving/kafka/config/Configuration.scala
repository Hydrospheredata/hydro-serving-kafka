package io.hydrosphere.serving.kafka.config

import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._


final case class KafkaConfiguration(
  advertisedHost: String,
  advertisedPort: Int,
  shadowTopic: String = "shadow_topic"
)
final case class ApplicationConfig(
  appId: String
)
final case class SidecarConfig(
  host: String,
  egressPort: Int,
  ingressPort: Int,
  adminPort: Int
)
final case class GrpcConfig(
  port: Int,
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