package io.hydrosphere.serving.kafka.stream

import java.util.Properties

import io.hydrosphere.serving.kafka.{AppAndStream, KeyAndStream, StreamFromApp}
import io.hydrosphere.serving.kafka.config.Configuration
import io.hydrosphere.serving.kafka.predict.{Application, UpdateService, Version}
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig}
import org.apache.kafka.streams.kstream.KStream

import scala.collection._



case class AppTopicsConfig(outTopic: String, errorTopic: Option[String])


class KafkaStreamer[K, V](keySerde: Class[_ <: Serde[K]], valSerde: Class[_ <: Serde[V]])(
  implicit config: Configuration,
  builder: StreamsBuilder,
  updateService: UpdateService[Seq[Application]])
  extends Streamer[K, V, Application, KafkaStreams] {


  override def stop(): Unit = {
    val stopped = store.map { case (app, stream) =>
      stream.close()
      app
    }

    stopped.foreach({ appName =>
      store.remove(appName)
      logger.info(s"Application streaming stopped:$appName")
    })
  }

  override protected[this] def streamFor[R](application: Application)
                                           (transform: KeyAndStream[K, V, Application] => DoubleStream[K, R]): Option[(Application, KafkaStreams)] = {

    for {
      in <- application.inTopic
      out <- application.outTopic
    } yield {
      val id = application.consumerId.getOrElse(application.name)
      val props = new Properties()
      props.put(StreamsConfig.APPLICATION_ID_CONFIG, id)
      props.put(StreamsConfig.CLIENT_ID_CONFIG, id)
      import org.apache.kafka.streams.StreamsConfig
      props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, s"${config.kafka.advertisedHost}:${config.kafka.advertisedPort}")
      props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, keySerde)
      props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valSerde)
      val kStream: KStream[K, V] = builder.stream(in)
      val stream = KafkaStream[K, V](kStream)
      val successAndFailure = transform((application, stream))
      val successTopic = out
      val errorTopic = application.errorTopic.getOrElse(successTopic)
      successAndFailure._1.to(successTopic)
      successAndFailure._2.to(errorTopic)
      val topology = builder.build()
      val streams = new KafkaStreams(topology, props)
      streams.start()
      logger.info(
        s"Application streaming started for $application")

      (application, streams)
    }
  }

  override def streamForAll[R]
  (stream: StreamFromApp[K, V, R]): Unit = {
    updateService.subscribe(updateStore(stream, updateService.getUpdates))
    updateService.getUpdates("0")
  }

  def itemsById(appName: String): Set[Application] = store.keySet
    .view
    .filter(_.name == appName)
    .toSet

  override def deleteAction(tuple: (Application, KafkaStreams)): Unit = {
    val (app, stream) = tuple
    stream.close()
    store -= app
    logger.info(s"Stopped application: $app")
  }
}



