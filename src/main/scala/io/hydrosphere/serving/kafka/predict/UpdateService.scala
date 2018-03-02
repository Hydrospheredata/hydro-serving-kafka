package io.hydrosphere.serving.kafka.predict

import java.util.concurrent.locks.ReentrantReadWriteLock


trait UpdateService[T] {

  private[this] val rwLock = new ReentrantReadWriteLock()
  private[this] var upsertHandlers = List[(T, Version) => Unit]()
  private[this] var deleteHandlers = List[(T, Version) => Unit]()
  private[this] var currentVersion = "0"

  def subscribe(handler:(T, Version) => Unit) = {
    val lock = rwLock.writeLock()
    try{
      lock.lock()
      upsertHandlers = upsertHandlers ::: List(handler)
    } finally {
      lock.unlock()
    }
  }

  def getVersion(): String ={
    val lock = rwLock.readLock()
    try{
      lock.lock()
      currentVersion
    } finally {
      lock.unlock()
    }
  }

  def setVersion(version:String): Unit ={
    val lock = rwLock.writeLock()
    try{
      lock.lock()
      currentVersion = version
    } finally {
      lock.unlock()
    }
  }

  def getUpdates()

  private[this] def doAction(item:T, version:Version, actions:List[(T, Version) => Unit]):Unit = {
    val lock = rwLock.readLock()
    var handlers:List[(T, Version) => Unit] = List()
    try{
      lock.lock()
      handlers = actions
      handlers foreach(f => f(item, version))
      currentVersion = version
    } finally {
      lock.unlock()
    }
  }

  protected def doOnNext(item:T, version:Version):Unit = doAction(item, version,  upsertHandlers)


}
