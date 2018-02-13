package io.hydrosphere.serving.kafka.config

import com.typesafe.config.ConfigFactory
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import io.hydrosphere.serving.kafka.mappers.KafkaServingMessageSerde
import io.hydrosphere.serving.kafka.predict.{PredictService, PredictServiceImpl, XDSApplicationUpdateService}
import org.apache.kafka.common.serialization.Serdes

object Inject{

  implicit val appConfig = Configuration(ConfigFactory.load())

  implicit val kafkaServing = new KafkaServingStream(Serdes.ByteArray().getClass, classOf[KafkaServingMessageSerde])

  implicit val rpcChanel: ManagedChannel = ManagedChannelBuilder
    .forAddress(appConfig.sidecar.host, appConfig.sidecar.port)
    .usePlaintext(true)
    .build

  implicit val predictService: PredictService = new PredictServiceImpl

  implicit val applicationUpdater = new XDSApplicationUpdateService()

}
