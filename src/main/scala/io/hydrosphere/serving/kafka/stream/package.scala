package io.hydrosphere.serving.kafka

package object stream {
  type DoubleStream[K,V] = (Stream[K,V], Stream[K,V])

}
