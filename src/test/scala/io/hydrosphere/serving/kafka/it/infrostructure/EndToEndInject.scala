package io.hydrosphere.serving.kafka.it.infrostructure

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import io.hydrosphere.serving.kafka.config._
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.mappers.KafkaServingMessageSerde
import io.hydrosphere.serving.kafka.predict.{PredictService, PredictServiceImpl, XDSApplicationUpdateService}
import io.hydrosphere.serving.kafka.stream.KafkaStreamer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder

object EndToEndInject {

  implicit val config = Configuration(
    ApplicationConfig("hydro-serving-kafka", 56789),
    SidecarConfig("localhost", 56788, 56688),
    KafkaConfiguration("localhost", 9092),
    GrpcConfig(9090)
  )

  implicit val chanel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 8081).usePlaintext.build
  implicit val predictService: PredictService = new PredictServiceImpl
  implicit val applicationUpdater = new XDSApplicationUpdateService()
  implicit lazy val streamsBuilder = new StreamsBuilder()
  implicit val streamer = new KafkaStreamer[Array[Byte], KafkaServingMessage](Serdes.ByteArray().getClass, classOf[KafkaServingMessageSerde])



}
