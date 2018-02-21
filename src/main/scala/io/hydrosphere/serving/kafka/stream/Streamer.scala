package io.hydrosphere.serving.kafka.stream

import io.hydrosphere.serving.kafka.AppAndStream

import scala.collection.Set


trait Streamer[K,V]{

  def stop():Unit
  def streamForAll[R](stream: AppAndStream[K,V] => DoubleStream[K,R]): Unit
  def inTopicsByApplicationName(appName:String):Set[String]

}

