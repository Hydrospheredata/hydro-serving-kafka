package io.hydrosphere.serving.kafka.predict

import java.util.concurrent.locks.ReentrantReadWriteLock

trait UpdateService[T] {

  private[this] val rwLock = new ReentrantReadWriteLock()
  private[this] var upsertHandlers = List[(T, Version) => Unit]()
  private[this] var deleteHandlers = List[(T, Version) => Unit]()

  def subscribe(handler:(T, Version) => Unit) = {
    val lock = rwLock.writeLock()
    try{
      lock.lock()
      upsertHandlers = upsertHandlers ::: List(handler)
    } finally {
      lock.unlock()
    }
  }

  def getUpdates(version:String)

  private[this] def doAction(item:T, version:Version, actions:List[(T, Version) => Unit]):Unit = {
    val lock = rwLock.readLock()
    var handlers:List[(T, Version) => Unit] = List()
    try{
      lock.lock()
      handlers = actions
    } finally {
      lock.unlock()
    }

    handlers foreach(f => f(item, version))
  }

  def doOnNext(item:T, version:Version):Unit = doAction(item, version,  upsertHandlers)

}
