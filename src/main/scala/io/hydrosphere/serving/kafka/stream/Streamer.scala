package io.hydrosphere.serving.kafka.stream

import java.util.concurrent.ConcurrentHashMap

import io.hydrosphere.serving.kafka.predict.Version
import io.hydrosphere.serving.kafka.{AppAndStream, KeyAndStream, StreamFromApp, StreamFromKey}
import org.apache.logging.log4j.scala.Logging

import scala.collection.{Seq, Set, concurrent}
import scala.collection.JavaConverters._


trait Streamer[K,V, StoreKey, StoreValue] extends Logging {

  def stop():Unit
  def streamForAll[R](stream: KeyAndStream[K,V, StoreKey] => DoubleStream[K,R]): Unit
  def itemsById(appName:String):Set[StoreKey]
  def deleteAction(tuple:(StoreKey, StoreValue)):Unit
  protected[this] def streamFor[R](storeKey: StoreKey)
                                  (transform: KeyAndStream[K, V, StoreKey] => DoubleStream[K, R]): Option[(StoreKey, StoreValue)]

  val store: concurrent.Map[StoreKey, StoreValue] =
    new ConcurrentHashMap[StoreKey, StoreValue]().asScala

  def updateStore[R](stream: StreamFromKey[K,V,R, StoreKey], afterUpdate: Version => Unit)
                    (newState:Seq[StoreKey], version:Version): Unit = {
    val newItems = newState.toSet
    val toDelete = store.filter{case (key, _) => !newItems.contains(key)}

    toDelete.foreach{ keyValue =>
      deleteAction(keyValue)
      store -= (keyValue._1)
    }

    newItems.foreach{ key => if(!store.contains(key)){
      streamFor(key)(stream).foreach{
        case (storeKey, storeVal) =>
          store += (storeKey -> storeVal)
      }

    }}

    afterUpdate(version)
  }

}

