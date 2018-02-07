package io.hydrosphere.serving

import io.hydrosphere.serving.kafka.predict.Application
import io.hydrosphere.serving.kafka.stream.Stream


package object kafka {
  type AppAndStream[K,V] = (Application, Stream[K, V])
}
