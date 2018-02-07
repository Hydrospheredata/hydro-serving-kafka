package io.hydrosphere.serving.kafka.stream

import io.hydrosphere.serving.kafka.AppAndStream
import io.hydrosphere.serving.kafka.predict.Application

trait Streamer[K,V]{

  def stop():Unit
  def init():Unit
  def getApplications():Seq[Application]
  def streamFor[R](application: Application, stream: AppAndStream[K,V] => DoubleStream[K,R]): Unit
  def streamForAll[R](stream: AppAndStream[K,V] => DoubleStream[K,R]): Unit = {
    getApplications().foreach{streamFor(_, stream)}
  }
  def stopStreamOf(app:Application):Unit
}

