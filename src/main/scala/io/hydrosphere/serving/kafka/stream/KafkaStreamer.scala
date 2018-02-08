package io.hydrosphere.serving.kafka.stream

import java.util.Properties

import io.hydrosphere.serving.kafka.AppAndStream
import io.hydrosphere.serving.kafka.config.Configuration
import io.hydrosphere.serving.kafka.predict.{Application, ApplicationService}
import io.hydrosphere.serving.kafka.utils.StoreWithLock
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.kstream.KStream
import org.apache.logging.log4j.scala.Logging

case class AppTopicsConfig(outTopic: String, errorTopic: Option[String])

class KafkaStreamer[K, V](keySerde: Class[_ <: Serde[K]], valSerde: Class[_ <: Serde[V]])(
  implicit config: Configuration, applicationService: ApplicationService)
  extends Streamer[K, V]
    with StoreWithLock[Application, KafkaStreams]
    with Logging {

  override def stop(): Unit = withReadLock(_.keySet).foreach(stopStreamOf(_))

  override def stopStreamOf(app: Application): Unit = withWriteLock(store => {
    store.get(app).foreach(stream => stream.close())
    store - app
  })

  override def getApplications(): Seq[Application] = applicationService
    .getApplications()

  override def streamFor[R](application: Application, transform: AppAndStream[K, V] => DoubleStream[K, R]): Unit = {
    application.inTopic
      .flatMap(i => application.outTopic
        .map(o => (i, o))).foreach { inAndOut =>
          val props = new Properties()
          props.put(StreamsConfig.APPLICATION_ID_CONFIG, application.name)
          props.put(StreamsConfig.CLIENT_ID_CONFIG, application.name)
          import org.apache.kafka.streams.StreamsConfig
          props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, s"${config.kafka.advertisedHost}:${config.kafka.advertisedPort}")
          props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, keySerde)
          props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valSerde)

          import org.apache.kafka.streams.StreamsBuilder
          val builder = new StreamsBuilder()
          val kStream: KStream[K, V] = builder.stream(inAndOut._1)
          val stream = KafkaStream[K, V](kStream)
          val successAndFailure = transform((application, stream))
          val topology = builder.build()
          val successTopic = inAndOut._2
          val errorTopic = application.errorTopic.getOrElse(successTopic)

          successAndFailure._1.to(successTopic)
          successAndFailure._2.to(errorTopic)
          val streams = new KafkaStreams(topology, props)
          withWriteLock(store => store + (application -> streams))
          streams.start()
    }
  }
}



