package io.hydrosphere.serving.kafka.config

import com.typesafe.config.ConfigFactory
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.mappers.{KafkaServingMessageSerde, KafkaServingMessageSerializer}
import io.hydrosphere.serving.kafka.predict.{PredictService, PredictServiceImpl, XDSApplicationUpdateService}
import io.hydrosphere.serving.kafka.stream.Producer
import org.apache.kafka.common.serialization.Serdes

object Inject {

  implicit lazy val appConfig = Configuration(ConfigFactory.load())

  implicit lazy val rpcChanel: ManagedChannel = ManagedChannelBuilder
    .forAddress(appConfig.sidecar.host, appConfig.sidecar.egressPort)
    .usePlaintext(true)
    .build

  implicit lazy val predictService: PredictService = new PredictServiceImpl
  implicit lazy val applicationUpdater = new XDSApplicationUpdateService()
  implicit lazy val kafkaServing = new KafkaServingStream(Serdes.ByteArray().getClass, classOf[KafkaServingMessageSerde])
  implicit lazy val kafkaProducer = Producer[Array[Byte], KafkaServingMessage](
    appConfig,
    Serdes.ByteArray().serializer().getClass,
    classOf[KafkaServingMessageSerializer])

}


