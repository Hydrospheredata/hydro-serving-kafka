package io.hydrosphere.serving.kafka.stream

import java.util.Properties

import io.hydrosphere.serving.kafka.{AppAndStream, StreamFromApp}
import io.hydrosphere.serving.kafka.config.Configuration
import io.hydrosphere.serving.kafka.predict.{Application, UpdateService, Version}
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.kstream.KStream
import org.apache.logging.log4j.scala.Logging

import scala.collection._
import scala.collection.convert.decorateAsScala._
import java.util.concurrent.ConcurrentHashMap

import envoy.api.v2.{DiscoveryRequest, Node}

case class AppTopicsConfig(outTopic: String, errorTopic: Option[String])




class KafkaStreamer[K, V](keySerde: Class[_ <: Serde[K]], valSerde: Class[_ <: Serde[V]])(
  implicit config: Configuration)
  extends Streamer[K, V]
    with Logging {

  val store: concurrent.Map[Application, KafkaStreams] =
    new ConcurrentHashMap[Application, KafkaStreams]().asScala


  override def stop(): Unit = {
    val stopped = store.map{case (app, stream) => {
      stream.close()
      app
    }}

    stopped.foreach({ appName =>
      store.remove(appName)
      logger.info(s"Application streaming stopped:$appName")
    })
  }

  private[this] def streamFor[R](application: Application)(transform: AppAndStream[K, V] => DoubleStream[K, R]): Unit = {
    application.inTopic
      .flatMap(i => application.outTopic
        .map(o => (i, o))).foreach { inAndOut =>
          val id = application.consumerId.getOrElse(application.name)
          val props = new Properties()
          props.put(StreamsConfig.APPLICATION_ID_CONFIG, id)
          props.put(StreamsConfig.CLIENT_ID_CONFIG, id)
          import org.apache.kafka.streams.StreamsConfig
          props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, s"${config.kafka.advertisedHost}:${config.kafka.advertisedPort}")
          props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, keySerde)
          props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valSerde)

          import org.apache.kafka.streams.StreamsBuilder
          val builder = new StreamsBuilder()
          val kStream: KStream[K, V] = builder.stream(inAndOut._1)
          val stream = KafkaStream[K, V](kStream)
          val successAndFailure = transform((application, stream))
          val successTopic = inAndOut._2
          val errorTopic = application.errorTopic.getOrElse(successTopic)
          successAndFailure._1.to(successTopic)
          successAndFailure._2.to(errorTopic)
          val topology = builder.build()
          val streams = new KafkaStreams(topology, props)
          streams.start()
          store + (application -> streams)
          logger.info(
            s"Application streaming started for ${application}")

    }
  }

  override def streamForAll[R]
  (updateService: UpdateService[Seq[Application]])
  (stream:  StreamFromApp[K,V,R]): Unit = {
    updateService.subscribe(updateStore(stream, updateService.getUpdates))
    updateService.getUpdates("0")
  }

  def updateStore[R](stream: StreamFromApp[K,V,R], afterUpdate: Version => Unit)
                    (newState:Seq[Application], version:Version): Unit = {
    val newApps = newState.toSet
    val toDelete = store.filter{case (app, stream) => !newApps.contains(app)}

    toDelete.foreach{case (app,stream) => {
      stream.close()
      store - app
      logger.info(s"Stopped application: $app")
    }}

    newApps.map{app => if(!store.contains(app)){
      streamFor(app)(stream)
    }}

    afterUpdate(version)
  }


}



