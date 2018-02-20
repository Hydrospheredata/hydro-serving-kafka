package io.hydrosphere.serving.kafka.stream


import org.apache.kafka.streams.kstream.ValueTransformer

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

trait Stream[-K, V] {

  def mapV[V1](f: V => V1): Stream[K, V1]
  def filterV(f: V => Boolean):Stream[K, V]
  def branchV(success: V => Boolean, failure: V => Boolean):DoubleStream[K, V]
  def mapAsync[V1](f: V => Future[V1])(implicit ec: ExecutionContext, duration: Duration = 60 seconds): Stream[K, V1] =
    mapV(value => Await.result(f(value), duration))

  def transformV[VR](getTransformer:() => ValueTransformer[V, VR]): Stream[K, VR]

  def withLog(logging: V => Unit): Stream[K, V] =
    mapV(value => {
      logging(value)
      value
    })

  def to(topicName:String):Unit
}
