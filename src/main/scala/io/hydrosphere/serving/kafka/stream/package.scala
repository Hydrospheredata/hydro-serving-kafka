package io.hydrosphere.serving.kafka

import io.hydrosphere.serving.kafka.predict.Application
import org.apache.kafka.streams.KafkaStreams

package object stream {

  type DoubleStream[K,V] = (Stream[K,V], Stream[K,V])
  type AppStreamer[K,V] = Streamer[K,V, Application, KafkaStreams]

}
