package io.hydrosphere.serving.kafka.config

import com.typesafe.config.ConfigFactory
import io.grpc._
import io.hydrosphere.serving.grpc.{AuthorityReplacerInterceptor, Headers}
import io.hydrosphere.serving.kafka.grpc.PredictionGrpcApi
import io.hydrosphere.serving.kafka.kafka_messages.KafkaServingMessage
import io.hydrosphere.serving.kafka.mappers.{KafkaServingMessageSerde, KafkaServingMessageSerializer}
import io.hydrosphere.serving.kafka.predict._
import io.hydrosphere.serving.kafka.stream.Producer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder

object Inject {

  implicit lazy val appConfig = Configuration(ConfigFactory.load())

  lazy val rpcChanel: ManagedChannel = ManagedChannelBuilder
    .forAddress(appConfig.sidecar.host, appConfig.sidecar.egressPort)
    .usePlaintext(true)
    .build

  implicit val channel: Channel = ClientInterceptors
    .intercept(rpcChanel, new AuthorityReplacerInterceptor +: Headers.interceptors: _*)

  implicit lazy val predictService: PredictService = new PredictServiceImpl
  implicit lazy val applicationUpdater = new XDSApplicationUpdateService()
  implicit lazy val streamsBuilder = new StreamsBuilder()
  implicit lazy val kafkaServing = new KafkaServingStream(Serdes.ByteArray().getClass, classOf[KafkaServingMessageSerde])
  implicit lazy val kafkaProducer = Producer[Array[Byte], KafkaServingMessage](
    appConfig,
    Serdes.ByteArray().serializer().getClass,
    classOf[KafkaServingMessageSerializer])

  implicit val predictionApi = new PredictionGrpcApi

}


