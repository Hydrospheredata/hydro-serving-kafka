package io.hydrosphere.serving.kafka.stream

import io.hydrosphere.serving.kafka.AppAndStream
import io.hydrosphere.serving.kafka.predict.{Application, UpdateService}


trait Streamer[K,V]{

  def stop():Unit
  def streamForAll[R](stream: AppAndStream[K,V] => DoubleStream[K,R]): Unit

}

