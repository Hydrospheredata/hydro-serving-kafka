package io.hydrosphere.serving.kafka.it.infrostructure

import akka.actor.ActorSystem
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import io.hydrosphere.serving.kafka.config._
import io.hydrosphere.serving.kafka.grpc.PredictionGrpcApi
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.mappers.{KafkaServingMessageSerde, KafkaServingMessageSerializer}
import io.hydrosphere.serving.kafka.predict.{PredictService, PredictServiceImpl, XDSApplicationUpdateService}
import io.hydrosphere.serving.kafka.stream.{KafkaStreamer, Producer}
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder

object TestInject {

  implicit val config = Configuration(
    ApplicationConfig(),
    SidecarConfig("localhost", 56788),
    KafkaConfiguration("localhost", 9092),
    GrpcConfig(56789)
  )

  implicit val sidecarConfig = config.sidecar
  implicit val as = ActorSystem()

  implicit val modelChanel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 56787).usePlaintext.build
  val appChanel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 56786).usePlaintext.build

  implicit val predictService: PredictService = new PredictServiceImpl

  implicit val applicationUpdater = new XDSApplicationUpdateService()

  implicit lazy val streamsBuilder = new StreamsBuilder()

  implicit val streamer = new KafkaStreamer[Array[Byte], KafkaServingMessage](Serdes.ByteArray().getClass, classOf[KafkaServingMessageSerde])

  implicit lazy val kafkaProducer = Producer[Array[Byte], KafkaServingMessage](
    config,
    Serdes.ByteArray().serializer().getClass,
    classOf[KafkaServingMessageSerializer])

  implicit val predictionApi = new PredictionGrpcApi


}
