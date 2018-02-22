package io.hydrosphere.serving


import io.hydrosphere.serving.kafka.predict.Application
import io.hydrosphere.serving.kafka.stream.Stream
import org.apache.kafka.streams.KafkaStreams


package object kafka {
  type AppAndStream[K,V] = (Application, Stream[K, V])
  type KeyAndStream[K,V, StoreKey] = (StoreKey, Stream[K, V])
  type StreamFromApp[K,V,R] = AppAndStream[K, V] => (Stream[K, R], Stream[K, R])
  type StreamFromKey[K,V,R, StoreKey] = KeyAndStream[K, V, StoreKey] => (Stream[K, R], Stream[K, R])
}
